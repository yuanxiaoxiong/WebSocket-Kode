package com.personal.push;

import java.io.Serializable;

/**
 * Description: 测试实体类
 * Created by kode on 2020/6/14.
 */
public class TestModel implements Serializable {
    private String id;

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
