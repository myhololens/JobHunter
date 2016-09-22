package org.codingwater.controller;

import org.codingwater.concurrency.CaculateThread;
import org.codingwater.controller.loadbalance.DynamicLB;
import org.codingwater.controller.loadbalance.Nodes;
import org.codingwater.controller.loadbalance.TestDynamicLB;
import org.codingwater.model.SalaryQueryResult;
import org.codingwater.service.IReportService;
import org.omg.PortableInterceptor.INACTIVE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by water on 4/26/16.
 */
@Controller
public class ChartsController {

  @Autowired
  IReportService reportService;

  final static String[] caption = { "fast", "common", "slow" };

  @RequestMapping(value = "/charts/", method = RequestMethod.GET)
  public String chartsView(Model model, @RequestParam(value = "city", defaultValue = "上海")String
      city, @RequestParam(value = "keyword", defaultValue = "Java")String
      keyword) {
    //查询多个不同工作经验的薪资水平.

    SalaryQueryResult result1 = reportService.calculateAverageSalary(city, keyword, "应届毕业生");
    SalaryQueryResult result2 = reportService.calculateAverageSalary(city, keyword, "1年以下");
    SalaryQueryResult result3 = reportService.calculateAverageSalary(city, keyword, "1-3年");
    SalaryQueryResult result4 = reportService.calculateAverageSalary(city, keyword, "3-5年");
    SalaryQueryResult result5 = reportService.calculateAverageSalary(city, keyword, "5-10年");

    model.addAttribute("result1", result1);
    model.addAttribute("result2", result2);
    model.addAttribute("result3", result3);
    model.addAttribute("result4", result4);
    model.addAttribute("result5", result5);
    return "charts";
  }

  @RequestMapping(value = "/loadbalance/", method = RequestMethod.GET)
  public String loadbalance(Model model, @RequestParam("profiles") String profiles,
          @RequestParam(value = "latestWindowSize", defaultValue = "100")int latestWS,
          @RequestParam(value = "avgWindowSize", defaultValue = "5000")int avgWS
          ) {

    String[] str = profiles.split(",");
    int array[] = new int[str.length];
    for (int i = 0; i < str.length; i++) {
      array[i] = Integer.parseInt(str[i]);
    }

    //直接调用LB
    DynamicLB dynamicLB = new DynamicLB(array.length, 1000, 5000);
    List<Integer> samples = new ArrayList<Integer>();

    //根据生成的累加概率积分进行随机采样，统计10000次中各个节点被选择的次数
    for (int j = 0; j < 10000; j++) {
      int pos = dynamicLB.sample();
      //做一万次采样
      samples.add(pos);
    }
    //对样品进行统计.
    //对采样数据整理
    Map<Integer, Long> result = samples.stream()
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

    String[] title = new String[array.length];

    List<Nodes> nodesList = new ArrayList<>();
    for (int j = 0; j < array.length; j++) {
      Nodes nodes = new Nodes();
      nodes.setCaption(caption[array[j]]);
      System.out.println(result.get(j).intValue());
      nodes.setCount(String.valueOf(result.get(j)));
      nodesList.add(nodes);

      title[j] = caption[array[j]];
    }


    for (Nodes node : nodesList) {
      System.out.println(node.getCaption() + "=>" + node.getCount());
    }
    model.addAttribute("nodes", nodesList);
    return "lb";
    }






  @RequestMapping(value = "/charts/multithread/", method = RequestMethod.GET)
  public String chartsViewWithCallable(Model model, @RequestParam(value = "city", defaultValue =
      "上海")String
      city, @RequestParam(value = "keyword", defaultValue = "Java")String
      keyword) {
    //查询多个不同工作经验的薪资水平.

    final ExecutorService service;
    final Future<?> task1;
    final Future<?> task2;
    final Future<?> task3;
    final Future<?> task4;
    final Future<?> task5;

    service = Executors.newFixedThreadPool(5);
    task1 = service.submit(new CaculateThread(city, keyword, "应届毕业生", this.reportService));
    task2 = service.submit(new CaculateThread(city, keyword, "1年以下", this.reportService));
    task3 = service.submit(new CaculateThread(city, keyword, "1-3年", this.reportService));
    task4 = service.submit(new CaculateThread(city, keyword, "3-5年", this.reportService));
    task5 = service.submit(new CaculateThread(city, keyword, "5-10年", this.reportService));

    try {

      // waits the 10 seconds for the Callable.call to finish.
      SalaryQueryResult result1 = (SalaryQueryResult) task1.get(); // this raises
      SalaryQueryResult result2 = (SalaryQueryResult) task2.get(); // this raises
      SalaryQueryResult result3 = (SalaryQueryResult) task3.get(); // this raises
      SalaryQueryResult result4 = (SalaryQueryResult) task4.get(); // this raises
      SalaryQueryResult result5 = (SalaryQueryResult) task5.get(); // this raises

      // ExecutionException if thread dies
//      System.out.println(result);
      model.addAttribute("result1", result1);
      model.addAttribute("result2", result2);
      model.addAttribute("result3", result3);
      model.addAttribute("result4", result4);
      model.addAttribute("result5", result5);
    } catch (final InterruptedException ex) {
      ex.printStackTrace();
    } catch (final ExecutionException ex) {
      ex.printStackTrace();
    }

    service.shutdownNow();

    return "charts";
  }



}



