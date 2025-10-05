package com.punks.recruitment.domain.exception;

import com.punks.recruitment.domain.route.Token;

public class NoRouteException extends DomainException {
	public NoRouteException(Token from, Token to) {
		super(String.format("No route from token: %s to token: %s", from.value(), to.value()));
	}
}
