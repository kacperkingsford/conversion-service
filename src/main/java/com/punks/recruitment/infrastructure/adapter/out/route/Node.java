package com.punks.recruitment.infrastructure.adapter.out.route;

import com.punks.recruitment.domain.route.Hop;
import com.punks.recruitment.domain.route.Token;

import java.util.List;

public record Node(Token token, List<Hop> path) {
}
