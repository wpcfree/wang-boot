package com.wpc.system.controller;

import com.wpc.base.controller.BaseController;
import com.wpc.system.model.Dict;
import com.wpc.system.service.IDictService;
import com.wpc.system.warpper.DictWarpper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * 字典控制器
 *
 * @author 王鹏程
 * @Date 2017年4月26日 12:55:31
 */
@Controller
@RequestMapping("/dict")
public class DictController extends BaseController {

    private String PREFIX = "/system/dict/";

    @Autowired
    private IDictService dictService;

    /**
     * 跳转到字典管理首页
     */
    @RequestMapping("")
    public String index() {
        return PREFIX + "dict";
    }

    /**
     * 跳转到添加字典
     */
    @RequestMapping("/dict_add")
    public String deptAdd() {
        return PREFIX + "dict_add";
    }

    /**
     * 跳转到修改字典
     */
    @RequestMapping("/dict_edit/{dictId}")
    public String deptUpdate(@PathVariable Long dictId, Model model) {
        Dict dict = dictService.findById(dictId);
        model.addAttribute("dict", dict);
        List<Dict> subDicts = dictService.selectByParentId(dictId);
        model.addAttribute("subDicts", subDicts);
//        LogObjectHolder.me().set(dict);
        return PREFIX + "dict_edit";
    }

    /**
     * 新增字典
     *
     * @param dictValues 格式例如   "1:启用;2:禁用;3:冻结"
     */
    @RequestMapping(value = "/add")
    @ResponseBody
    public Object add(String dictCode, String dictTips, String dictName, String dictValues) {
//        if (ToolUtil.isOneEmpty(dictCode, dictName, dictValues)) {
//            throw new ServiceException(BizExceptionEnum.REQUEST_NULL);
//        }
        this.dictService.addDict(dictCode, dictName, dictTips, dictValues);
        return SUCCESS_TIP;
    }

    /**
     * 获取所有字典列表
     */
    @RequestMapping(value = "/list")
    @ResponseBody
    public Object list(String condition) {
        List<Map<String, Object>> list = this.dictService.list(condition);
        return new DictWarpper(list).wrap();
    }

    /**
     * 字典详情
     */
    @RequestMapping(value = "/detail/{dictId}")
    @ResponseBody
    public Object detail(@PathVariable("dictId") Long dictId) {
        return dictService.findById(dictId);
    }

    /**
     * 修改字典
     */
    @RequestMapping(value = "/update")
    @ResponseBody
    public Object update(Integer dictId, String dictCode, String dictName, String dictTips, String dictValues) {
//        if (ToolUtil.isOneEmpty(dictId, dictCode, dictName, dictValues)) {
//            throw new ServiceException(BizExceptionEnum.REQUEST_NULL);
//        }
        dictService.editDict(dictId, dictCode, dictName, dictTips, dictValues);
        return SUCCESS_TIP;
    }

    /**
     * 删除字典记录
     */
    @RequestMapping(value = "/delete")
    @ResponseBody
    public Object delete(@RequestParam Integer dictId) {

        //缓存被删除的名称
//        LogObjectHolder.me().set(ConstantFactory.me().getDictName(dictId));

        this.dictService.delteDict(dictId);
        return SUCCESS_TIP;
    }

}
