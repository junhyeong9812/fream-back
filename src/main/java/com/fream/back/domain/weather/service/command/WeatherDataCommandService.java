package com.fream.back.domain.weather.service.command;

//날씨 데이터 "쓰기(등록/수정/삭제)" 관련 기능을 정의하는 Service 인터페이스.
public interface WeatherDataCommandService {


    // 외부 API에서 24시간 날씨 데이터를 가져와 DB에 저장한다.
    void fetchAndStore24HourWeatherData();
}
