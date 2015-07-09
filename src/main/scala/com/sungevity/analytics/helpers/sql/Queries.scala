package com.sungevity.analytics.helpers.sql

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

object Queries {

  val sqlDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd");

  val allSystems =
    """
      |SELECT
      |		i.Account_Number__c  AS 'accountNumber',
      |		i.Country__c         AS 'country',
      |		i.Latitude__c        AS 'latitude',
      |		i.Longitude__c       AS 'longitude',
      |		s.Id                 AS 'systemID',
      |		a.Id                 AS 'arrayID',
      |		a.Pitch__c           AS 'pitch',
      |		a.Azimuth__c         AS 'azimuth',
      |		a.Standoff_Height__c AS 'standoffHeight',
      |		a.Jan__c,
      |		a.Feb__c,
      |		a.Mar__c,
      |		a.Apr__c,
      |		a.May__c,
      |		a.Jun__c,
      |		a.Jul__c,
      |		a.Aug__c,
      |		a.Sep__c,
      |		a.Oct__c,
      |		a.Nov__c,
      |		a.Dec__c,
      |		pr1.sungevity_id__c  AS 'inverterID',
      |		pr2.sungevity_id__c  AS 'moduleID',
      |		a.Module_Quantity__c AS 'moduleQuantity',
      |		a.Shared_Inverter__c AS 'isInverterShared'
      |
      |
      |		FROM
      |		salesforce_prod.Milestone1_Project__c           AS p
      |		LEFT JOIN salesforce_prod.Tranche__c            AS t   ON t.Id = p.Tranche__c
      |		LEFT JOIN salesforce_prod.iQuote__c             AS i   ON p.Id = i.Project__c
      |		LEFT JOIN salesforce_prod.System__c             AS s   ON i.Id = s.iQuote__c
      |		LEFT JOIN salesforce_prod.Array__c              AS a   ON s.Id = a.System__c
      |		LEFT JOIN salesforce_prod.Sungevity_Products__c AS pr1 ON a.Inverter__c = pr1.Id
      |		LEFT JOIN salesforce_prod.Sungevity_Products__c AS pr2 ON a.Module__c = pr2.Id
      |
      |
      |		WHERE
      |		s.ABS__c = 'True'
      |		AND p.Final_Inter_Approved__c IS NOT NULL
      |		AND i.Country__c = 'US'
      |		AND p.Status__c NOT IN ( 'Test', 'Cancelled')
      |		AND t.Name <> 'Test Project'           # Test tranche used by software
      |		AND a.Jun__c IS NOT NULL
      |
    """.stripMargin

  def systemData(start: DateTime, end: DateTime, nDays: Int) =
    s"""
      |SELECT
      |
      | a.AccountNumber AS 'accountNumber',
      | p.Name AS 'name',
      | p.Site_State_Province__c AS 'state',
      | i.Latitude__c            AS 'latitude',
      | i.Longitude__c           AS 'longitude',
      | SUM(l.reading)           AS 'actualKwh',
      | COUNT(l.reading) / COUNT( DISTINCT l.meter_id)       AS 'count',
      | c.Subject                AS 'openCase',
      | CONVERT( s.System_Performance_Notes__c USING utf8 ) AS 'pgNotes',
      |
      | CASE
      |	WHEN (p.PG_VOID_del__c = 'TRUE' AND p.PG_Void_Reason__c IS NULL) THEN '(no reason entered)'
      |	WHEN  p.PG_VOID_del__c = 'TRUE'                                  THEN p.PG_Void_Reason__c
      |	ELSE ''
      |
      |  END AS 'pgVoid',
      |
      | p.Final_Inter_Approved__c AS 'interconnectionDate'
      |
      |
      |FROM salesforce_prod.Account AS a
      |
      |LEFT JOIN salesforce_prod.Milestone1_Project__c AS p ON ( p.Account__c = a.Id )
      |LEFT JOIN salesforce_prod.iQuote__c                             AS i ON ( i.Project__c = p.Id )
      |LEFT JOIN salesforce_prod.System__c                             AS s ON ( s.iQuote__c = i.Id AND s.ABS__c = 'TRUE' )
      |LEFT JOIN locus.locus_staging                   AS l ON
      |(
      |	l.customer_id = a.AccountNumber
      |	AND ( l.meter_id LIKE '%.1'   OR   l.meter_id LIKE 'VALHALLA%' )
      |)
      |
      |LEFT JOIN
      |(
      |	SELECT
      |	  cs.Project__c,
      |	 cs.Subject
      |
      |	FROM
      |	salesforce_prod.`Case` as cs
      |
      |	WHERE (
      |		cs.`Status` <> 'Closed'
      |		OR cs.ClosedDate > '${sqlDateFormat.print(start)}'
      |	)
      |	AND cs.`Type` IN (
      |		  'Equipment Failure',
      |		'Leak Investigation',
      |		'Internal Error',
      |		'Monitoring Connectivity',
      |		'Environmental Damage',
      |		'Electrical/Mechanical',
      |		'Electrical / Mechanical',
      |		'Monitoring Approval',
      |		'Lease Transfer','Customer Escalation'
      |	)
      |) AS c ON (c.Project__c = p.Id)
      |
      |
      |
      |WHERE
      |    l.reading_date >= '${sqlDateFormat.print(start)}'
      |AND l.reading_date <= '${sqlDateFormat.print(end)}'
      |
      |AND p.Final_Inter_Approved__c < '${sqlDateFormat.print(start)}'
      |
      |GROUP BY a.AccountNumber
      |
    """.stripMargin

  def productionData(start: DateTime, end: DateTime) =
    s"""
      |SELECT
      |		l.customer_id AS 'accountNumber',
      |		l.reading_date AS readingDate,
      |		sum(l.reading) AS reading
      |
      |
      |		FROM
      |		locus.locus_staging AS l
      |
      |  WHERE
      |		( l.meter_id LIKE '%.1' OR l.meter_id LIKE 'VALHALLA%' )
      |		AND l.reading_date >= '${sqlDateFormat.print(start)}'
      |		AND l.reading_date <= '${sqlDateFormat.print(end)}'
      |   AND l.reading_date is not NULL
      |
      |		GROUP BY l.reading_date, l.customer_id
    """.stripMargin

}



