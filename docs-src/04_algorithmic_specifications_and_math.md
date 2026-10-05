# Chapter 4: Algorithmic Specifications & Mathematical Foundations

## 4.1 Overview of Core Algorithmic Engines
The algorithmic integrity of H8 resides exclusively within the `common` module. All scoring functions are deterministic, mathematical formulations designed to minimize patient mortality, eliminate clinical mismatches, and preserve metropolitan emergency coverage.

---

## 4.2 Multi-Factor Candidate Scoring Engine (`DispatchScorer`)

### 4.2.1 Objective Function & Factor Weighting
For an incident $i$ located at $(\lambda_i, \phi_i)$ and candidate ambulance unit $u \in \mathcal{U}_{\text{avail}}$, the dispatch score $S(u, i) \in [0, 1]$ is computed as a weighted linear combination of five normalized clinical and operational components:

$$S(u, i) = \alpha \cdot f_{\text{ETA}}(u, i) + \beta \cdot f_{\text{cap}}(u, i) + \gamma \cdot f_{\text{fatigue}}(u) + \delta \cdot f_{\text{cov}}(u) + \epsilon \cdot f_{\text{stale}}(u)$$

Where the normalized weights satisfy the convex constraint:
$$\alpha + \beta + \gamma + \delta + \epsilon = 1.0$$

The empirical weights tuned for metropolitan emergency medical response are:

| Component Weight | Parameter | Value | Clinical & Operational Rationale |
| :--- | :--- | :--- | :--- |
| **Proximity / ETA Weight** | $\alpha$ | **0.35** | Ensures fast arrival within urban target thresholds (8 minutes). |
| **Capability Matching Weight**| $\beta$ | **0.45** | Highest priority: guarantees appropriate medical equipment on scene. |
| **Crew Fatigue Weight** | $\gamma$ | **0.08** | Prevents burnout and paramedic diagnostic error on extended shifts. |
| **Coverage Loss Weight** | $\delta$ | **0.07** | Penalizes stripping the last active unit from a suburban quadrant. |
| **Telemetry Staleness Weight** | $\epsilon$ | **0.05** | Penalizes units with outdated GPS fixes that may have moved. |

---

### 4.2.2 Component Formulations

#### 1. Proximity / ETA Component $f_{\text{ETA}}(u, i)$
ETA is mapped through a monotonically decreasing sigmoid function bounded between $[0, 1]$, where faster arrival yields a higher score:

$$f_{\text{ETA}}(u, i) = \max\left(0, 1 - \frac{\text{ETA}(u, i)}{\text{ETA}_{\text{max}}}\right)$$

Where $\text{ETA}_{\text{max}} = 1800 \text{ seconds}$ (30 minutes). An estimated arrival time of 0 seconds produces $f_{\text{ETA}} = 1.0$, while an arrival time exceeding 30 minutes produces $0.0$.

#### 2. Clinical Capability Matching Component $f_{\text{cap}}(u, i)$
The capability component directly prevents dangerous clinical under-triaging:

$$f_{\text{cap}}(u, i) = 1.0 - P_{\text{mismatch}}(u, i)$$

Where $P_{\text{mismatch}}(u, i) \in [0, 1]$ is the capability penalty computed from the clinical matrix:

```
+-----------------------------------------------------------------------------------------+
|                               CLINICAL MISMATCH PENALTY MATRIX                          |
+--------------------------+--------------------+-------------------+---------------------+
| Incident Clinical Need   | Incident Severity  | Unit Type Dispatched| Penalty Value (P) |
+--------------------------+--------------------+-------------------+---------------------+
| Any (requiresAls = true) | CRITICAL / URGENT  | BLS (Basic)       | 0.55                |
| CARDIAC (STEMI / Arrest) | CRITICAL           | BLS (Basic)       | 0.65                |
| TRAUMA (Hemorrhage)      | CRITICAL           | BLS (Basic)       | 0.40                |
| STROKE (Neuro-window)    | URGENT             | BLS (Basic)       | 0.45                |
| PEDIATRIC                | CRITICAL           | BLS (Basic)       | 0.50                |
| Any                      | STANDARD           | BLS (Basic)       | 0.00                |
| Any                      | Any                | ALS (Advanced)    | 0.00                |
+--------------------------+--------------------+-------------------+---------------------+
```

*Note: An ALS unit is a superset capable of handling both BLS and ALS calls. Thus, dispatching an ALS unit to a BLS incident incurs no capability penalty ($P = 0.0$), but the coverage component ensures scarce ALS units are preserved unless no BLS unit is available.*

#### 3. Crew Fatigue Component $f_{\text{fatigue}}(u)$
Paramedic decision-making degrades significantly after consecutive mission assignments without rest. The fatigue component is modeled as:

$$f_{\text{fatigue}}(u) = 1.0 - \min\left(1.0, \frac{N_{\text{missions}}(u)}{N_{\text{max}}} + \frac{T_{\text{active}}(u)}{T_{\text{shift}}}\right)$$

Where:
* $N_{\text{missions}}(u)$ is the count of emergency runs completed in the current 12-hour shift ($N_{\text{max}} = 8$).
* $T_{\text{active}}(u)$ is the cumulative minutes spent on active transports without an intervening 30-minute rest cycle ($T_{\text{shift}} = 720 \text{ minutes}$).

#### 4. Coverage Loss Component $f_{\text{cov}}(u)$
When unit $u$ is dispatched from station $s$, the remaining fleet coverage for the surrounding demand area $A_s$ decreases:

$$f_{\text{cov}}(u) = \frac{|\mathcal{U}_{\text{available}} \cap \text{Radius}(u, 8\text{km})|}{K_{\text{target}}}$$

Where $K_{\text{target}}$ is the target backup unit density (default: 2 units within 8 km). If unit $u$ is the sole remaining ambulance in its zone, removing it leaves zero backup coverage, yielding $f_{\text{cov}}(u) = 0.0$.

#### 5. Telemetry Staleness Component $f_{\text{stale}}(u)$
To prevent dispatching "ghost" vehicles that have disconnected from the mobile network:

$$f_{\text{stale}}(u) = \exp\left(-\frac{\Delta t_{\text{last\_fix}}}{T_{\text{decay}}}\right)$$

Where $\Delta t_{\text{last\_fix}} = t_{\text{now}} - t_{\text{telemetry}}$ in seconds, and $T_{\text{decay}} = 30.0 \text{ seconds}$.
* If $\Delta t \le 5\text{s}$: $f_{\text{stale}} \approx 1.00$ (Full confidence).
* If $\Delta t = 30\text{s}$: $f_{\text{stale}} = e^{-1} \approx 0.368$ (Penalized).
* If $\Delta t \ge 60\text{s}$: $f_{\text{stale}} \approx 0.00$ (Stale flag triggered).

---

## 4.3 Double-Standard Coverage Optimization (`CoverageModel` / MEXCLP)

The platform evaluates urban readiness utilizing the **Maximum Expected Coverage Location Problem (MEXCLP)** framework.

### 4.3.1 Mathematical Formulation
Let $\mathcal{I}$ represent the set of demand nodes (metropolitan census zones) with demand weights $d_i$, and $\mathcal{J}$ represent candidate base stations. Let $p$ be the total fleet size, and $q \in (0, 1)$ be the system-wide ambulance busy probability:

$$q = \frac{\sum_{i \in \mathcal{I}} d_i \cdot \bar{t}_{\text{service}}}{p \cdot 86400}$$

Where $\bar{t}_{\text{service}}$ is the mean incident service duration (typically ~45 minutes = 2,700 seconds).

The expected covered demand $E[\text{Coverage}]$ with $k$ backup ambulances is:

$$\max \sum_{i \in \mathcal{I}} d_i \sum_{k=1}^{p} (1 - q) q^{k-1} y_{ik}$$

Subject to:
$$\sum_{j \in \mathcal{N}_i} x_j \ge \sum_{k=1}^{p} y_{ik}, \quad \forall i \in \mathcal{I}$$
$$\sum_{j \in \mathcal{J}} x_j = p$$
$$x_j \in \mathbb{Z}^+, \quad y_{ik} \in \{0, 1\}$$

Where:
* $x_j$ is the number of ambulances stationed at base $j$.
* $\mathcal{N}_i = \{j \in \mathcal{J} : \text{dist}(i, j) \le R_{\text{target}}\}$ is the neighborhood within response threshold $R_{\text{target}} = 8 \text{ km}$.
* $y_{ik} = 1$ if demand node $i$ is covered by at least $k$ ambulances.

---

## 4.4 Receiving Hospital Destination Ranking (`DestinationRanker`)

When an on-scene crew initiates transport, the system evaluates all receiving facilities $h \in \mathcal{H}$ to determine the optimal emergency department:

$$R(h, p) = w_{\text{cap}} \cdot C(h, p) + w_{\text{time}} \cdot T(h, p) + w_{\text{div}} \cdot D(h) + w_{\text{bed}} \cdot B(h)$$

```
                                  DESTINATION RANKING WEIGHTS
                              +---------------------------------+
                              | Clinical Match   (w_cap) : 0.40 |
                              | Travel Time      (w_time): 0.30 |
                              | Diversion Status (w_div) : 0.20 |
                              | Bed Availability (w_bed) : 0.10 |
                              +---------------------------------+
```

Where:
* $C(h, p) \in \{0.0, 1.0\}$: Binary clinical capability match. If patient $p$ has acute stroke symptoms, $C(h, p) = 1.0$ only if hospital $h$ has an operational CT/MRI and neurology on-call.
* $T(h, p) = \max\left(0, 1 - \frac{\text{TravelSeconds}(h)}{1800}\right)$: Proximity travel score.
* $D(h)$: Diversion penalty. If hospital $h$ has declared internal disaster or ER saturation diversion, $D(h) = 0.0$; otherwise $1.0$.
* $B(h) = \frac{\text{FreeEDBeds}(h)}{\text{TotalEDBeds}(h)}$: Fractional bed headroom.

---

## 4.5 Spatial Calculation & Geodesic Formulations

### 4.5.1 Haversine Distance (Great-Circle)
For two coordinates $(\phi_1, \lambda_1)$ and $(\phi_2, \lambda_2)$ in radians, distance $d$ in kilometers is:

$$\Delta\phi = \phi_2 - \phi_1, \quad \Delta\lambda = \lambda_2 - \lambda_1$$
$$a = \sin^2\left(\frac{\Delta\phi}{2}\right) + \cos(\phi_1)\cos(\phi_2)\sin^2\left(\frac{\Delta\lambda}{2}\right)$$
$$c = 2 \cdot \text{atan2}\left(\sqrt{a}, \sqrt{1 - a}\right)$$
$$d_{\text{haversine}} = R_{\text{earth}} \cdot c \quad (R_{\text{earth}} = 6371.0 \text{ km})$$

### 4.5.2 Urban Tortuosity & Road Network Calibration
In urban road grids, actual road distance $d_{\text{road}}$ exceeds great-circle distance by a tortuosity factor $\tau$:

$$d_{\text{road}} = \tau \cdot d_{\text{haversine}}, \quad \tau_{\text{jaipur}} = 1.35$$
$$\text{ETA}_{\text{fallback}} = \frac{d_{\text{road}}}{v_{\text{emergency}}} \cdot 3600 \quad (v_{\text{emergency}} = 40.0 \text{ km/h})$$
