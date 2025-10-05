package com.punks.recruitment.application.service.convert;

import com.punks.recruitment.application.service.convert.dto.RouteQuery;
import com.punks.recruitment.config.properties.RouteQueryProperties;
import org.springframework.stereotype.Component;

@Component
public class RouteQueryFactory {
	private final RouteQueryProperties props;

	public RouteQueryFactory(RouteQueryProperties props) {
		this.props = props;
	}

	public RouteQuery defaultQuery() {
		return new RouteQuery(props.getMaxHops(), props.getMaxPriceAge());
	}
}
