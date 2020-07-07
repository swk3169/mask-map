package com.example.maskapp;

import java.util.Comparator;

public class Store implements Comparable<Store> {
    public String addr; // 주소
    public String code; // 식별 코드
    public String created_at;   // 데이터 생성 일자($YYYY/MM/DD HH:mm:ss)
    public double lat;  // 위도
    public double lng;  // 경도
    public String name; // 이름
    public String remain_stat;  // 재고 상태[100개 이상(녹색): 'plenty' / 30개 이상 100개미만(노랑색): 'some' / 2개 이상 30개 미만(빨강색): 'few' / 1개 이하(회색): 'empty' / 판매중지: 'break']
    public String stock_at; // 입고시간($YYYY/MM/DD HH:mm:ss)
    public String type; // 판매처 유형[약국: '01', 우체국: '02', 농협: '03' ]

    public int getAmount() {
        if ("plenty".equalsIgnoreCase(remain_stat)) {
            return 0;
        } else if ("some".equalsIgnoreCase(remain_stat)) {
            return 1;
        } else if ("few".equalsIgnoreCase(remain_stat)) {
            return 2;
        } else if ("empty".equalsIgnoreCase(remain_stat)) {
            return 3;
        } else if ("break".equalsIgnoreCase(remain_stat)) {
            return 4;
        } else {
            return 5;
        }
    }

    @Override
    public int compareTo(Store store) {
        return getAmount() - store.getAmount();
    }

    public static class NameSorter implements Comparator<Store> {
        public int compare(Store store1, Store store2) {
            store1.name = (store1.name == null) ? "" : store1.name;
            store2.name = (store2.name == null) ? "" : store2.name;
            return store1.name.compareTo(store2.name);
        }
    }

    public static class StockAtSorter implements Comparator<Store> {
        public int compare(Store store1, Store store2) {
            store1.stock_at = (store1.stock_at == null) ? "" : store1.stock_at;
            store2.stock_at = (store2.stock_at == null) ? "" : store2.stock_at;
            return store1.stock_at.compareTo(store2.stock_at);
        }
    }
}
