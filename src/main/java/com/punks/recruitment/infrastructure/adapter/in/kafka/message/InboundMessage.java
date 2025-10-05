package com.punks.recruitment.infrastructure.adapter.in.kafka.message;

public sealed interface InboundMessage permits ConvertTokenMessage, MarketEventMessage { }
