package com.sungevity.analytics.helpers.sql

object Queries {

  val selectAllSystems =
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

}
