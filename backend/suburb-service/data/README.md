# Data Files — Manual Downloads Required

Place these files in this folder before running the weekly refresh scheduler.
Files are gitignored — never commit real data files.

---

## 1. bocsar_crime.csv
**Source:** NSW Bureau of Crime Statistics and Research
**URL:** https://www.bocsar.nsw.gov.au/Pages/bocsar_crime_stats/bocsar_local_crime_tool.aspx
**Required columns:** `suburb`, `offence_category`, `incidents_per_100k`

---

## 2. nsw_rent.xlsx
**Source:** NSW Dept of Communities & Justice — Rent and Sales Reports
**URL:** https://www.facs.nsw.gov.au/resources/statistics/rent-and-sales
**Required columns:** `postcode`, `bedrooms`, `dwelling_type`, `median_rent_weekly`

---

## 3. nsw_property_sales.csv
**Source:** NSW Valuer General — Property Sales Information
**URL:** https://valuation.property.nsw.gov.au/embed/propertySalesInformation
**Required columns:** `suburb`, `property_type`

---

## Notes
- The weekly scheduler runs every Sunday at 2am and reads these files automatically
- If a file is missing, that data source is skipped (service continues normally)
- Trigger manually via `POST /api/suburbs/admin/load-schools` for school data
- For crime, rent, and property data, restart or wait for Sunday scheduler
