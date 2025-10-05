package com.punks.recruitment.application.port.out.clock;

import java.time.Instant;

public interface ClockPort {
	Instant now();
}
