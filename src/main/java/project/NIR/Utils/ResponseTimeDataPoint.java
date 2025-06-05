package project.NIR.Utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResponseTimeDataPoint {
    private final long timestamp;
    private final long responseTime;
} 