package com.wpc.base.service;

import com.github.pagehelper.PageInfo;
import com.wpc.base.entity.DataEntity;
import com.wpc.base.entity.datatables.DataTablesRequest;
import com.wpc.base.entity.datatables.DataTablesResponse;

import java.util.List;

/**
 * 功能描述: service层接口基础类，封装了一些基本的方法
 * @Author: 王鹏程
 * @E-mail: wpcfree@qq.com @QQ: 376205421
 * @Blog: http://www.wpcfree.com
 * @Date:
 */
public interface BaseService<T extends DataEntity<T>> {

    void insert(T t);

    void update(T t);

    void delete(Long id);

    void deleteByIds(Long[] ids);

    T findById(Long id);

    List<T> queryAll();

    T selectOne(T query);
    
    List<T> search(T query);

    PageInfo<T> search(T query, int pageNum, int pageSize);
    
    List<T> query(T query);
    
    DataTablesResponse<T> searchPage(DataTablesRequest query);

    Integer count();

    Integer count(T t);

}
