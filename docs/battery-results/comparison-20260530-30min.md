# StepSync Battery Comparison - 2026-05-30 30-Minute Run

Device: `R5CX51AQ71R`

Duration: 30 minutes per run

Screen state: screen off for about 99.7% of both runs

## Result

| Metric | Baseline | Optimized | Change |
| --- | ---: | ---: | ---: |
| App CPU estimated power | 1.17 mAh | 0.248 mAh | 78.8% lower |
| App total estimated power | 1.19 mAh | 0.410 mAh | 65.5% lower |
| Screen-on time | 4.62 seconds | 5.18 seconds | Comparable |
| Screen-off time | 29m 55.7s | 29m 55.0s | Comparable |
| Optimized tracking mode | n/a | HARDWARE_STEP_COUNTER | n/a |
| Optimized sensor delay | n/a | 1,000,000 us | n/a |
| Optimized UI update interval | n/a | 5,000 ms | n/a |
| Optimized notification interval | n/a | 15,000 ms | n/a |

## Interpretation

The optimized build reduced StepSync-attributed CPU power from `1.17 mAh` to `0.248 mAh`, a `78.8%` reduction in a 30-minute background BatteryStats test.

The total app power reduction was `65.5%` because Android also attributed wakelock power to the optimized app. For README and resume wording, the cleanest defensible claim is the CPU-power result, because that maps directly to the app-side optimization: fewer wakeups, batched UI broadcasts, throttled notification updates, and low-frequency hardware step-counter registration.

Both runs used Android's hardware step counter path. This was not a GPS-vs-pedometer comparison.

## Suggested Resume Claim

```text
Reduced StepSync tracking CPU power by 78.8% in a 30-minute background Android BatteryStats test by adding Power Saver Tracking with batched UI updates, throttled foreground notifications, and low-frequency hardware step-counter registration.
```

Rounded version:

```text
Reduced StepSync tracking CPU power by nearly 80% in 30-minute background Android BatteryStats testing by adding Power Saver Tracking with batched UI updates, throttled foreground notifications, and low-frequency hardware step-counter registration.
```
