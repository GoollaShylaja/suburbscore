# SuburbScore — Problem Statement

## What Problem Does It Solve?

Moving to Sydney means researching dozens of suburbs across multiple
websites — with no tool that scores suburbs based on your personal
priorities. SuburbScore fixes that.

---

## The Real Problems People Face Today

### 1. 10 websites, no single answer
People moving to Sydney visit Domain, realestate.com.au, Crime Map,
My School, Transport NSW, OpenStreetMap, and council websites separately
— and still cannot compare suburbs side by side in one place.

**Real cost:** 10–15 hours of manual research per person.

---

### 2. Generic results, not personalised
Every existing suburb tool ranks by median price or generic ratings.
They have no concept of YOUR priorities — commute time, school quality,
safety, parks, or budget. A young professional and a family with kids
need completely different suburbs.

**Real cost:** Wrong suburb chosen. Wrong school zone. Wrong commute.

---

### 3. Commute not calculated for your workplace
No existing tool calculates actual transit time from a suburb to YOUR
specific workplace. No tool accounts for couples with two different
workplaces needing to find a midpoint suburb.

**Real cost:** Couples underserved completely. Commute guessed not calculated.

---

### 4. Rent data ignores your property needs
Every suburb shows one "median rent" figure. But a 3-bedroom house vs
a 1-bedroom apartment can differ by $500/week in the same suburb.
Generic median rent is meaningless without knowing bedroom count and
property type.

**Real cost:** Budget decisions made on wrong data.

---

### 5. Newcomers and migrants are completely lost
International migrants, students from overseas, and interstate movers
have zero local knowledge of Sydney suburbs. They choose based on
guesswork, word-of-mouth, or advice from real estate agents who have
a financial conflict of interest.

**Real scale:** 3 million+ temporary visa holders in Australia navigating
this exact problem.

---

### 6. No side-by-side suburb comparison tool exists
Comparing two suburbs requires opening two browser tabs and manually
checking each data point one by one. No dedicated suburb comparison
tool exists in Australia today.

**Real cost:** Decisions made on incomplete, unstructured comparisons.

---

## How SuburbScore Solves Each Problem

### Solution 1 — One personalised score per suburb (0–100)
Aggregates real data from:
- Transport NSW Trip Planner API (commute times)
- BOCSAR NSW Crime Statistics (safety)
- NSW Department of Education Open Data API (school quality)
- OpenStreetMap Overpass API (parks, walkability)
- NSW DCJ Quarterly Rent Data (rent by bedroom + type)
- NSW Property Sales Data (property type mix)

All in one dashboard. No tab-switching required.

---

### Solution 2 — Weighted scoring based on YOUR priorities
User sets importance of each dimension on a 1–5 scale:
- Commute time
- Safety
- School quality
- Walkability
- Parks and green space
- Value for money

Algorithm weights the final score accordingly. A family
automatically gets higher weight on schools and safety.
A professional gets higher weight on commute.

---

### Solution 3 — Real commute time to ANY workplace
Uses Transport NSW Trip Planner API to calculate actual
transit time from each of 660 Sydney suburbs to the user's
specific workplace address.

For couples: calculates commute for BOTH workplaces and
averages the score. First tool in Australia to support
dual-workplace suburb scoring.

Results cached in Redis for 30 days so subsequent users
searching the same route get instant results.

---

### Solution 4 — Rent matched to exact needs
Value score uses NSW DCJ quarterly rental bond data filtered
to the user's specific requirements:
- Exact bedroom count (1, 2, 3, 4, 5+)
- Exact property type (house, apartment, townhouse, unit)

A family needing a 3-bedroom house sees rent data relevant
to 3-bedroom houses in each suburb — not a mixed median.

---

### Solution 5 — Interactive Sydney map
All 660 Greater Sydney suburbs displayed on an interactive
Leaflet.js map, colour-coded by personalised score:
- Dark green: 80–100 (excellent match)
- Light green: 60–79 (good match)
- Yellow: 40–59 (moderate match)
- Orange: 20–39 (poor match)
- Red: 0–19 (not suitable)

Instantly see which geographic areas of Sydney suit you
without reading through a list.

---

### Solution 6 — Side-by-side suburb comparison
Compare any two suburbs across all 7 scoring dimensions:
- Winner highlighted per category
- Personalised recommendation generated
- "Suburb A is better for you because safety is your top priority
  and Mosman scores 9.2/10 vs Bankstown's 4.1/10"

Australia's first dedicated suburb comparison tool.

---

### Solution 7 — Rent change alerts
Users save suburbs they are considering. When NSW DCJ rent
data refreshes quarterly and a saved suburb's median rent
changes by more than 5%, the user receives an automatic
email alert.

Built using Apache Kafka event streaming — ingestion service
publishes a data-updated event, alert service consumes it,
checks saved suburbs, sends targeted emails.

---

## Who Benefits

| User Type | Problem | How SuburbScore Helps |
|---|---|---|
| New migrants | No local knowledge | Data-driven shortlist in minutes |
| Couples | Two workplaces, different priorities | Dual-workplace scoring |
| Families | Need schools + parks + safety | Auto-boosts family priorities |
| Interstate movers | Zero Sydney suburb knowledge | Instant personalised ranking |
| Pet owners | Need parks + houses with gardens | Parks score boosted automatically |
| Professionals | Optimise commute vs rent trade-off | Ranked list by personalised score |

---

## Market Context — Why This Matters in Sydney

- **5.6 million** people in Greater Sydney
- **660** official suburbs to choose from
- **3 million+** temporary visa holders in Australia
- **$650/week** median Sydney rent — highest in Australia
- **10+** websites currently required to research one suburb
- **0** personalised suburb scoring tools exist today

Sydney has the most expensive rental market in Australia.
Choosing the wrong suburb costs thousands of dollars in rent,
hours of lost commute time daily, and months of disruption
when you have to move again. The stakes are high and the
tools available today are completely inadequate.

---

## What Makes SuburbScore Different

### vs realestate.com.au / Domain
Great for finding properties. Zero suburb intelligence.
No scoring, no comparison, no personalisation, no commute
calculation, no school or crime data at suburb level.

SuburbScore is COMPLEMENTARY — users still use Domain to
find specific properties, but use SuburbScore to choose
which SUBURB to search in first.

### vs Homely suburb reviews
Community opinions and ratings. Subjective, unstructured,
no data-driven scoring, no personalisation. Cannot tell
you if a suburb suits YOUR commute and budget specifically.

SuburbScore is DATA-DRIVEN vs opinion-driven.

### vs ChatGPT / AI assistants
Can answer questions about suburbs but has:
- No real-time data
- No persistent memory of your preferences
- No map visualisation
- No rent alerts
- Cannot score all 660 suburbs at once against your priorities

SuburbScore is PERSISTENT, DATA-FRESH, and VISUAL.

---

## Technical Approach

Built with enterprise-grade architecture to demonstrate
real-world engineering capability:

- **8 Spring Boot 3 microservices** (Java 21)
- **Spring Cloud Gateway** (API routing, JWT validation)
- **Netflix Eureka** (service discovery)
- **Apache Kafka** (event-driven rent alerts)
- **Redis** (intelligent caching — commute routes cached 30 days)
- **PostgreSQL + PostGIS** (geospatial suburb data)
- **React 18 + TypeScript + Tailwind CSS** (frontend)
- **Leaflet.js** (interactive Sydney map)
- **Recharts** (score breakdown radar charts)
- **Railway.app + Vercel** (cloud deployment, fully live)
- **GitHub Actions** (CI/CD pipeline)

All data sourced from real, free Australian government APIs
and datasets — no mocked data in production.

---

## One-Line Summary for Resume / LinkedIn

> "SuburbScore is a Sydney suburb intelligence platform that
> scores all 660 Greater Sydney suburbs from 0–100 based on
> each user's personal priorities — commute, safety, schools,
> parks, and budget — using real data from Transport NSW,
> BOCSAR, and NSW Government APIs, built with Java 21
> microservices, Apache Kafka, and React."

---

*SuburbScore — Built in Sydney, for Sydney. 2026*
