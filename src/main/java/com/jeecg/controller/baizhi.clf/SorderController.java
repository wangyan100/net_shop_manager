package com.jeecg.controller.baizhi.clf;

import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jeecg.entity.baizhi.clf.SorderItemEntity;
import com.jeecg.entity.baizhi.clf.SproductEntity;
import com.jeecg.entity.baizhi.clf.SuserEntity;
import com.jeecg.service.baizhi.clf.SorderItemServiceI;
import com.jeecg.service.baizhi.clf.SproductServiceI;
import com.jeecg.service.baizhi.clf.SuserServiceI;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import org.jeecgframework.core.common.controller.BaseController;
import org.jeecgframework.core.common.hibernate.qbc.CriteriaQuery;
import org.jeecgframework.core.common.model.json.AjaxJson;
import org.jeecgframework.core.common.model.json.DataGrid;
import org.jeecgframework.core.constant.Globals;
import org.jeecgframework.core.util.StringUtil;
import org.jeecgframework.tag.core.easyui.TagUtil;
import org.jeecgframework.web.system.pojo.base.TSDepart;
import org.jeecgframework.web.system.service.SystemService;
import org.jeecgframework.core.util.MyBeanUtils;

import com.jeecg.entity.baizhi.clf.SorderEntity;
import com.jeecg.service.baizhi.clf.SorderServiceI;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.jeecgframework.core.beanvalidator.BeanValidators;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.net.URI;

import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author zhangdaihao
 * @version V1.0
 * @Title: Controller
 * @Description: 订单表
 * @date 2018-03-14 14:59:51
 */
@Controller
@RequestMapping("/sorderController")
public class SorderController extends BaseController {
    /**
     * Logger for this class
     */
    private static final Logger logger = Logger.getLogger(SorderController.class);

    @Autowired
    private SorderServiceI sorderService;
    @Autowired
    private SystemService systemService;
    @Autowired
    private Validator validator;
    @Autowired
    private SorderItemServiceI sorderItemService;
    @Autowired
    private SuserServiceI suserServiceI;
    @Autowired
    private SproductServiceI sproductServiceI;


    /**
     * 订单表列表 页面跳转
     *
     * @return
     */
    @RequestMapping(params = "list")
    public ModelAndView list(HttpServletRequest request) {
        return new ModelAndView("com/jeecg/baizhi.clf/sorderList");
    }

    /**
     * easyui AJAX请求数据
     *
     * @param request
     * @param response
     * @param dataGrid
     * @param user
     */

    @RequestMapping(params = "datagrid")
    public void datagrid(SorderEntity sorder, HttpServletRequest request, HttpServletResponse response, DataGrid dataGrid) {
        CriteriaQuery cq = new CriteriaQuery(SorderEntity.class, dataGrid);
        //查询条件组装器
        org.jeecgframework.core.extend.hqlsearch.HqlGenerateUtil.installHql(cq, sorder, request.getParameterMap());
        this.sorderService.getDataGridReturn(cq, true);
        List<SorderEntity> results = (List<SorderEntity>) dataGrid.getResults();

        List<SorderEntity> results2 = new ArrayList<SorderEntity>();
        //扩展字段集合
        HashMap<String, Map<String, Object>> extMap = new HashMap<>();
        for (SorderEntity od : results) {

            //通过订单获取用户信息
            SuserEntity user = suserServiceI.getEntity(SuserEntity.class, od.getUserId());

            //获取该订单的所有订单项
            List<SorderItemEntity> orderItems = sorderItemService.findByProperty(SorderItemEntity.class, "orderId", od.getId());

        /*    if(od.getOrderStatus().equals("Y")){
                od.setOrderStatus("已处理");
            }else{
                od.setOrderStatus("待处理");

            }*/
            //每行所扩展的字段
            HashMap<String, Object> map = new HashMap<String, Object>();

            map.put("userMsg", user.getUsername());
                /*//通过订单项获得商品信息
				SproductEntity sproduct  = sproductServiceI.getEntity(SproductEntity.class, orderItem.getProductId());
				map.put("productMsg",sproduct.getName());
				//该商品价格 跟 店长所设置价格一致
				map.put("productPrice",orderItem.getPrice());*/

            //添加扩展字段
            extMap.put(od.getId(), map);
            //每行数据
            results2.add(od);

        }
        //把更改的数据覆盖原有的数据
        dataGrid.setResults(results2);

        TagUtil.datagrid(response, dataGrid, extMap);
    }

    /**
     * 删除订单表
     *
     * @return
     */
    @RequestMapping(params = "del")
    @ResponseBody
    public AjaxJson del(SorderEntity sorder, HttpServletRequest request) {
        String message = null;
        AjaxJson j = new AjaxJson();
        sorder = systemService.getEntity(SorderEntity.class, sorder.getId());
        message = "订单表删除成功";
        sorderService.delete(sorder);
        systemService.addLog(message, Globals.Log_Type_DEL, Globals.Log_Leavel_INFO);

        j.setMsg(message);
        return j;
    }


    /**
     * 添加订单表
     *
     * @param ids
     * @return
     */
    @RequestMapping(params = "save")
    @ResponseBody
    public AjaxJson save(SorderEntity sorder, HttpServletRequest request) {
        String message = null;
        AjaxJson j = new AjaxJson();
        if (StringUtil.isNotEmpty(sorder.getId())) {
            message = "订单表更新成功";
            SorderEntity t = sorderService.get(SorderEntity.class, sorder.getId());
            try {
                MyBeanUtils.copyBeanNotNull2Bean(sorder, t);
                sorderService.saveOrUpdate(t);
                systemService.addLog(message, Globals.Log_Type_UPDATE, Globals.Log_Leavel_INFO);
            } catch (Exception e) {
                e.printStackTrace();
                message = "订单表更新失败";
            }
        } else {
            message = "订单表添加成功";
            sorderService.save(sorder);
            systemService.addLog(message, Globals.Log_Type_INSERT, Globals.Log_Leavel_INFO);
        }
        j.setMsg(message);
        return j;
    }

    /**
     * 订单表列表页面跳转
     *
     * @return
     */
    @RequestMapping(params = "addorupdate")
    public ModelAndView addorupdate(SorderEntity sorder, HttpServletRequest req) {
        if (StringUtil.isNotEmpty(sorder.getId())) {
            sorder = sorderService.getEntity(SorderEntity.class, sorder.getId());
            req.setAttribute("sorderPage", sorder);
        }
        return new ModelAndView("com/jeecg/baizhi.clf/sorder");
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public List<SorderEntity> list() {
        List<SorderEntity> listSorders = sorderService.getList(SorderEntity.class);
        return listSorders;
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<?> get(@PathVariable("id") String id) {
        SorderEntity task = sorderService.get(SorderEntity.class, id);
        if (task == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity(task, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> create(@RequestBody SorderEntity sorder, UriComponentsBuilder uriBuilder) {
        //调用JSR303 Bean Validator进行校验，如果出错返回含400错误码及json格式的错误信息.
        Set<ConstraintViolation<SorderEntity>> failures = validator.validate(sorder);
        if (!failures.isEmpty()) {
            return new ResponseEntity(BeanValidators.extractPropertyAndMessage(failures), HttpStatus.BAD_REQUEST);
        }

        //保存
        sorderService.save(sorder);

        //按照Restful风格约定，创建指向新任务的url, 也可以直接返回id或对象.
        String id = sorder.getId();
        URI uri = uriBuilder.path("/rest/sorderController/" + id).build().toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uri);

        return new ResponseEntity(headers, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(@RequestBody SorderEntity sorder) {
        //调用JSR303 Bean Validator进行校验，如果出错返回含400错误码及json格式的错误信息.
        Set<ConstraintViolation<SorderEntity>> failures = validator.validate(sorder);
        if (!failures.isEmpty()) {
            return new ResponseEntity(BeanValidators.extractPropertyAndMessage(failures), HttpStatus.BAD_REQUEST);
        }

        //保存
        sorderService.saveOrUpdate(sorder);

        //按Restful约定，返回204状态码, 无内容. 也可以返回200状态码.
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") String id) {
        sorderService.deleteEntityById(SorderEntity.class, id);
    }

    @RequestMapping(params = "openOrderItem")
    public String openProductDetail(String id,HttpServletRequest request) {
        request.setAttribute("id",id);

        return "com/jeecg/baizhi.clf/sorderItems";
    }
    @RequestMapping(params = "findOrderItem")
    public void  findOrderItem(SorderEntity sorderEntity,HttpServletRequest request, HttpServletResponse response, DataGrid dataGrid){
        //通过订单获取订单项数据
        List<SorderItemEntity> orderItems = sorderItemService.findByProperty(SorderItemEntity.class, "orderId", sorderEntity.getId());

        //封装商品数据
        ArrayList<SproductEntity> products = new ArrayList<>();

        //通过订单项获取该订单的所有商品
        for (SorderItemEntity orderItem : orderItems) {
            SproductEntity product = sproductServiceI.getEntity(SproductEntity.class, orderItem.getProductId());
            products.add(product);
        }

        //覆盖原有的数据
        dataGrid.setResults(products);

        TagUtil.datagrid(response,dataGrid);
    }
    @RequestMapping(params = "changeStatus")
    @ResponseBody
    public void changeStatus(SorderEntity sorder,HttpServletRequest request) {
        String status = sorder.getOrderStatus();
        if(status.equals("已处理")){

            //sorder.setOrderStatus("N");
            sorder.setOrderStatus("待处理");

        }else{
            //sorder.setOrderStatus("Y");
            sorder.setOrderStatus("已处理");
        }
        sorderService.saveOrUpdate(sorder);
    }


}