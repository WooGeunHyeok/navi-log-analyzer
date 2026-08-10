 package com.navi.loganalyzer.backend.domain.parsing.entity;

public enum LogLayer {

    CONTAINER,              // C++ Container 영역
    JNI_LAYER,              // JNI 바인딩 레이어
    JAVA_LAYER,             // 시스템 영역 (Android Java)
    NULL
}