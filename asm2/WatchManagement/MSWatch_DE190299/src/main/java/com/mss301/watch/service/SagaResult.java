package com.mss301.watch.service;

// [TODO-6.14: wrapper kết quả trả về kèm sagaId]
public record SagaResult<T>(String sagaId, T body) {}
