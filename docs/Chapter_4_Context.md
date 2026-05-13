# Chapter 4: A Reactive Approach for Taking Condition-Based Maintenance into Account in the Joint Scheduling of Production and Maintenance

In this chapter, we present our first contribution in the field of joint scheduling of production and maintenance, addressing the case of Condition-Based Maintenance (CBM). Based on our literature review, we noticed a lack of studies concerned with the scheduling of this type of maintenance. In fact, these works focus on researching the parameters that allow for the detection of a potential failure occurrence. However, they do not provide guidance on the scheduling of Condition-Based Maintenance (CBM) to avoid the failure.

In reality, Condition-Based Maintenance (CBM) is programmed preventive maintenance following the detection of an operational anomaly reported using tools relied upon by the production control system. Unlike corrective maintenance, the machine here remains operational. It is only necessary to choose the appropriate time to intervene in order to minimize the disruption to the ongoing production plan as much as possible.

In this work, we propose a two-level solution: 
1. **Single-machine level**, where the plan of the machine requiring intervention is modified to insert the new CBM maintenance; and 
2. **Multi-machine level**, where the production plans of neighboring machines are updated if they are affected by the first level.

## 1. Introduction

We consider a multi-machine workshop where each machine $M_i$ executes a joint schedule consisting of production tasks and regular/systematic maintenance activities (TBM). The execution of these tasks is controlled by a production system using sensors. These sensors are connected to a computer running a decision support software. The software compares the collected data with those defining the normal state of the machines and acts in case of an anomaly.

To avoid a failure in the "subject machine" (MS), a preventive maintenance activity $CBM_i$ must be imperatively programmed on it. It will be denoted as $CBM_{ij}$ and must therefore be inserted into the current schedule of the machine MS.

The Remaining Useful Life ($RUL$), as previously introduced in Chapter 3, is the remaining operating time of the equipment. Its estimation is essential for predicting and managing machine conditions. It is a rough estimate of the appearance of a functional failure in the machine. The $RUL$ is therefore considered a deadline before which action must be taken to return the machine to a good state to avoid its complete shutdown and the subsequent disruption of the production process.

After the execution of the production plan begins, the collected data from the machine MS indicates a functional anomaly (Point "P" - Figure 4.1-a), which implies a high probability of failure (Point "F" - Figure 4.1-a).

> **Figure 4.1. P-F Curve and Adopted Solution Strategies**
> *(Figure Description)*:
> The figure is divided into two superimposed graphs:
> * **(a) Upper Graph**: The Y-axis represents the "machine operating condition" and the X-axis represents time ($t$). A descending curve shows the degradation of the machine. We see a point identifying "potential failure « P »" at time $t_0$, representing the "identification of the need for CBM". Further along the curve is the "functional failure « F »" point. There is a specific yellow region between $t_1$ and $\text{DdIF}_i$, representing the "P-F interval".
> * **(b) Lower Graph**: The Y-axis represents "Machines" (specifically $MS$) and the X-axis represents time ($t$). It shows the timeline on machine $MS$ with blocks representing "TBM activities" and "production tasks". We see an "S.O.M" period (between $t_i$ and $\text{DdID}_i$) and an "S.O.P" period (between $\text{DdID}_i$ and $\text{DdIF}_i$).

After diagnosis, i.e., determining the required $CBM_{ij}$, it must be inserted into the current schedule of machine MS. Ideally, $CBM_{ij}$ should be planned before the end of $RUL_i$, i.e., within the interval $[t_1, t_i]$.

If $CBM_{ij}$ is programmed after $RUL_{ij}$, failures are likely to occur and therefore the machine will be unavailable. Since $t_i$ (the end of $RUL_{ij}$) is imprecise, we assume it can vary between two values $\text{DdID}_j$ (start of the deadline) and $\text{DdIF}_j$ (end of the deadline) (Figure 4.1-a).

Every maintenance activity (TBM or CBM) requires competence for its execution. The duration of the maintenance activity depends on the skill level and performance of the intervening human resources. The *performance* of the interveners depends on several parameters such as workload, their health condition, and their mood. Therefore, it becomes difficult to estimate the time the intervener will spend executing the maintenance activity. As a result, we assume that the intervener themselves estimates this time. *Availability* and *competence* are the other two constraints related to human resources that must strictly be taken into account when preparing schedules. In fact, since work has already started in the workshop, the interveners' schedules are predefined. Therefore, for $CBM_{ij}$ to be executed, a *qualified* and *available* intervener must be assigned.

Choosing the start date of $CBM_{ij}$ is very difficult because production is in progress. Interrupting the production process may cause completion delays and thus disrupt the plan of all machines. To provide more flexibility to decision-makers and find a balance between the risk of failure and production delays, we propose two solution strategies to manage machine maintenance: (1) the Maintenance-Oriented Strategy (*S.O.M*) and (2) the Production-Oriented Strategy (*S.O.P*) (Figure 4.1-b).

1. **Maintenance-Oriented Strategy (S.O.M)** means that $CBM_{ij}$ must be inserted in the interval $[t_1, \text{DdIS}_j]$ in order to not risk machine failure.
2. **Production-Oriented Strategy (S.O.P)** favors production tasks over maintenance activities. In this strategy, the risk of machine failure is tolerated in order to maintain the stability of the production plan. In this case, $CBM_{ij}$ is inserted in the interval $[\text{DsID}_i, \text{DdIF}_i]$.

## 2. Problem Formulation

In the following, we present the production scheduling problem, and the planning problem of maintenance activities considering human resources and the objective functions to be optimized.

### 2.1. Production Scheduling Problem

Let $M=\{M_1, M_2... M_{m0}\}$ be a set of machines and $P=\{P_1, P_2... P_{m1}\}$ be a set of production tasks scheduled on M. Each production task $P_k$ on machine $M_i$ is characterized by: release date $r_{ik}$, start time $t_{ik}$, processing time $p_{ik}$, completion time $c_{ik}$, and due date $d_{ik}$.
The processing times are constant and positive. We also assume that each task can be processed on at most one machine at a time, the operations constituting them are non-preemptive, and setup times can be estimated within the processing time of each operation.

### 2.2. Planning Preventive Maintenance Activities Considering Human Resource Constraints

Each $\text{TBM}_{il} \{l=1,...m_2\}$ planned on machine $M_i$ is characterized by a start time $t'_{il}$ and a completion time $c'_{il}$.

Since CBM is planned only on demand, i.e., after the detection of an operating anomaly, it is linked to the event of receiving signals from sensors. Each $\text{CBM}_{ij} \{j=1,...m_3\}$ is characterized by a processing time $p'_{ij}$, a required competence to execute it $cr_j$, and a deadline $RUL_i$ that must be respected to guarantee the availability of machine $M_i$. In reality, $p'_{ij}$ and $RUL_i$ cannot be known exactly. For this reason, we adopted an approximation using Fuzzy sets. $p'_{ij}$ and $RUL_i$ are modeled by the fuzzy sets $\widetilde{p'}_{ij}$ and $\widetilde{RUL}_i$ using a triangular membership function defined by the triplet $(v_i^1, v_i^2, v_i^3)$ (Figure 4.2.a).

The fuzzy nature of the processing time $\widetilde{p'}_{ij}$ for $\text{CBM}_{ij}$ affects its completion time $(\widetilde{c'}_{ij})$ and the boundaries of the time windows for the tasks that follow it. The execution time windows for production and maintenance tasks are modeled by a trapezoidal membership function (Figure 4.2.b). The fuzzy nature of the $\widetilde{RUL}_i$ value only affects the decision regarding the planning of $CBM_{ij}$ (see Section 3).

> **Figure 4.2. Fuzzy modeling of processing time, RUL, and task time windows**
> *(Figure Description)*:
> * **(a) Left Graph (- 2.a -)**: Shows a triangular fuzzy membership function.
> * **(b) Right Graph (- 2.b -)**: Shows trapezoidal membership functions representing time windows.

Each human resource in the maintenance department $HR_m \{m=1,..., m_4\}$ is characterized by $CR_m=\{cr_{m1}, cr_{m2}, ...\}$ which is a set of competencies qualifying them to execute maintenance activities, a set of intervals $ID_m=\{ID_{m1}, ID_{m2}, ...\}$ representing their available times, and a performance level $Perform_m$ determining the time it will take them to execute their activities.

Each human resource $HR_m$ can propose their average estimate $p'_{mp}$ for the processing time (Equation 1). It is then expressed as a triangular fuzzy number $\widetilde{p'}_{mp}$.

$$p'_{mp} = (p'/Cr_{mp}) \times Perform_m \quad (1)$$

### 2.3. Objective Functions

The objective of the proposed approach is to insert a new CBM into an existing joint schedule taking into account the competence, availability, and performance constraints of human resources. The main goal is to minimize disruptions to the existing schedule while ensuring machine availability.

Minimizing disruption means that the time windows of the existing production and TBM activities should be altered as little as possible. For these reasons, we propose partial rescheduling instead of total rescheduling to ensure the stability of the current production plan.

The production objective $\widetilde{f_1}$ is to minimize the average tardiness of production tasks (Equation 2). The total tardiness $\widetilde{T}_{ik}$ for task $P_k$ is calculated as follows (Equation 3):

$$\widetilde{f_1} = \frac{1}{m_1} \sum_{i=0}^{m_1} \widetilde{T}_{ik} \quad (2)$$

$$\widetilde{T}_{ik} = \max(0, ((\widetilde{c}_{ik}) - d_{ik})) \quad (3)$$

The functional failure of the machine MS depends on the date $t'_{ij}$ of planning the $CBM_{ij}$. We call the period between $t_p$ and $t'_{ij}$ the CBM delay (Equation 4). The risk of failure increases as the maintenance delay increases.

$$f_2 = \max(0, (t'_{ij} - t_p)) \quad (4)$$

In conclusion, the problem is to minimize the global objective function $\widetilde{f}$ (Equation 5) defined as a weighted sum of the average production tardiness $\widetilde{f_1}$ and the CBM delay called $f_2$.

$$
\begin{cases} 
\widetilde{f} = w_1 \widetilde{f_1} + w_2 f_2 \\ 
w_1 + w_2 = 1 
\end{cases} \quad (5)
$$

The weights $w_1$ and $w_2$ respectively measure the impact of production tardiness and CBM delay on the value. Their values depend on the adopted maintenance strategy (SOM or SOP).

## 3. Solution Approach

In our work, we propose a two-level solution: (1) single-machine level; and (2) multi-machine level. We use Agents such as AMS, AVA, AMA, AMC, ASRH, and ARH to model the actors in the workshop (Subject Machine, Downstream Machine, Upstream Machine, Maintenance Agent, Human Resources Supervisor, Human Resources Agents).

### 3.2. Single-Machine Level

In this section, we describe the local solution for AMS, i.e., how AMS proceeds to insert the CBM into its current production plan. This solution consists of 7 steps:

1. **Initialization Step:** AMS receives signals and sends an analysis request to AMC.
2. **Analysis Step:** AMC identifies the anomaly and estimates the $\text{RUL}_i$.
3. **Call for Proposal Step:** ASRH chooses a maintenance strategy and sends a Call for Proposals (CFP) to available and qualified ARHs.
4. **Proposal Formulation Step:** Each $\text{ARH}_m$ responds with their proposed processing time and availability (Algorithm 4.1).
5. **Reception Step:** ASRH receives positive responses and forwards them.
6. **Scheduling and Evaluation Step:** AMS evaluates the proposals and partially reschedules production tasks to minimize $(\widetilde{f_1})$ (Algorithm 4.2).
7. **Finalization Step:** AMS sends its decision so the chosen ARH is notified and schedules are updated.

---

### **Algorithm 4.1.** Behavior of $\text{ARH}_m$

**Let $\widetilde{p'}_{jm}$ be the time proposed by $\text{HRA}_m$ to execute $\text{CBM}_{ij}$...**
**Case 1.** If $\text{ARH}_m$ receives a CFP request:
    * **Step 1.** Computes the processing time.
    * **Step 2.** Inserts the availability periods into the proposal list `Prop_list`.
    * **Step 3.** Sends the list to ASRH.
**Case 2.** If it receives an intervention confirmation, it updates its local intervention plan.

---

### **Algorithm 4.2.** Behavior of AMS

**AMS identifies all possible insertion positions for CBM, and shifts or swaps production tasks to calculate and minimize the global objective function $\widetilde{f}$.**

### 3.2.8. Illustrative Example

We consider an AMS with a plan represented in Figure 4.5. Table 4.1 summarizes the initial data.

**Table 4.1. Data related to the initial schedule of machine MS**

| Order | Task ID | $p_i$ | $t_i$ | $c_i$ | $r_i$ | $d_i$ | HR ID | Maint. Type |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | P$_4$ | 20 | 4 | 24 | 0 | 60 | / | / |
| 2 | P$_3$ | 46 | 24 | 70 | 24 | 40 | / | / |
| 3 | M$_1$ | 20 | 70 | 90 | / | / | 2 | TBM |
| 4 | P$_2$ | 6 | 90 | 96 | 39 | 93 | / | / |
| 5 | M$_2$ | 7 | 96 | 103 | / | / | 1 | TBM |
| 6 | P$_5$ | 21 | 103 | 124 | 20 | 110 | / | / |

*(Figures 4.6 and 4.7 and Tables 4.2, 4.3, and 4.4 detail how the machine schedule is modified step by step based on the proposals of various human resource agents and using different strategies).*

**Table 4.5. Comparison of Human Resource Agent (ARH) Proposals**

| HRA | HRA$_1$ | HRA$_2$ |
| --- | --- | --- |
| $\widetilde{f1}$ = Average Tardiness | 36.33 | (21,22,23) |
| f$_2$ = CBM Delay | 16 | 95 |
| $\widetilde{f}$ = 0.75 * f$_1$ + 0.25 * f$_2$ | 31.2475 | (39.5,40.25,41) |
| Chosen ARH | X |  |

### 3.3. Multi-Machine Level

After the local solution of AMS, its new schedule may impact the schedule of neighboring machines, creating conflicts:

* **Upstream Conflict:** If the start date of a task on the machine precedes its completion date on the previous machine.
* **Downstream Conflict:** If the completion date of a task on the machine exceeds its start date on the next machine.

Negotiation processes (using `M_Message` and `I_Message`) are triggered to absorb these conflicts along the production line.

## 4. Experimental Results

Experiments were conducted to test the effectiveness of our approach using the ETOMA platform.

### 4.1. Test Protocol

Task and maintenance data were generated based on benchmarks (Taillard, 1993) using Equations (6 to 14) to generate release dates, due dates, and processing times.

### 4.2. Simulation Results

**4.2.1. System Reactivity (Table 4.6)**
Results showed that the multi-machine resolution time is always smaller compared to the single-machine resolution time, meaning that most disruptions are absorbed locally by the subject machine without heavily impacting its neighborhood. The resolution time increases with the number of tasks but remains reasonable.

**4.2.2. Approach's Capability to Absorb Average Production Tardiness (Table 4.7)**
In more than 60% of cases, production tardiness improved after inserting maintenance using the proposed rescheduling. In 70% of cases, the global factory tardiness (multi-machine) remained unchanged, proving that the objective of minimizing disruptions was achieved. The Maintenance-Oriented Strategy (SOM) provided the best balance between reducing the risk of failure and reducing production tardiness.

## 5. Conclusion

In this chapter, we presented a reactive model responding to the event of inserting a CBM into an ongoing production plan. We proposed partial rescheduling to ensure stability. Uncertainties were taken into account by introducing a fuzzy estimate for the machine's RUL and the CBM processing time.
Results demonstrated the single-machine local approach's capability to absorb disruption and reduce tardiness. In the next chapter, we will present our second contribution which addresses scheduling under uncertainty in a proactive and reactive manner.
