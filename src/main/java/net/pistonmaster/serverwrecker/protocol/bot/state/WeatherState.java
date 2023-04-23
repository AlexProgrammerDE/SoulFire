package net.pistonmaster.serverwrecker.protocol.bot.state;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@RequiredArgsConstructor
public final class WeatherState {
    private boolean raining;
    private float rainStrength;
    private float thunderStrength;
}
