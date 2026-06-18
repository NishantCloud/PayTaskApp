package com.qolorco.paytask.models;

public class TaskModel {
    public String taskId, name, link, status, tutorial;
    public double price;
    public boolean isHeader;

    public TaskModel(String name, boolean isHeader) {
        this.name = name;
        this.isHeader = isHeader;
    }

    public TaskModel(String taskId, String name, double price, String link, String status, String tutorial) {
        this.taskId = taskId;
        this.name = name;
        this.price = price;
        this.link = link;
        this.status = status;
        this.tutorial = tutorial;
        this.isHeader = false;
    }
}

