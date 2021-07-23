package com.lq.check;

//Sensitive 敏感的 'sensətɪv'

import androidx.annotation.NonNull;

public class SensitiveApiInfo {
    public String classFullName;

    @Override
    public String toString() {
        return "SensitiveApiInfo{" +
                "classFullName='" + classFullName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", desc='" + desc + '\'' +
                '}';
    }

    public String methodName;
    public String desc;

    public SensitiveApiInfo(String classFullName, String methodName, String desc) {
        this.classFullName = classFullName;
        this.methodName = methodName;
        this.desc = desc;
    }
}
