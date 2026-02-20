# Reproducer: DetachedCriteria.clone() does not copy connectionName

**Issue:** (will be linked after creation)

**Grails:** 7.0.7 | **GORM Hibernate:** via grails-data-hibernate5

## Bug Summary

AbstractDetachedCriteria.clone() copies criteria, projections, orders, defaultMax, defaultOffset, fetchStrategies, and joinTypes - but NOT connectionName. This means withConnection() settings are silently lost whenever a chained method calls clone() internally.

### Affected Methods

Any method that calls clone() internally loses the connection:
- where() / whereLazy()
- max()
- offset()
- sort()

### Root Cause

In AbstractDetachedCriteria.groovy (line 873-885), clone() copies 8 fields but omits connectionName (line 65), alias (line 64), lazyQuery (line 63), and associationCriteriaMap (line 66).

### Fix

Add the missing field copies to clone():
```groovy
criteria.connectionName = this.connectionName
criteria.alias = this.alias
criteria.lazyQuery = this.lazyQuery
criteria.@associationCriteriaMap = new HashMap<>(this.associationCriteriaMap)
```

## How to Reproduce

```bash
./gradlew integrationTest
```

### Expected Result

Tests should fail once the bug is fixed. The specs assert the buggy behavior by ensuring that chained where(), max(), and offset() calls lose the connectionName and revert to the default datasource.

### Actual Result

Tests pass because AbstractDetachedCriteria.clone() drops the connectionName, so queries run against the default datasource instead of the intended secondary datasource.

### Project Structure

```
grails-app/
  conf/application.yml                   - Two H2 datasources: default + secondary
  domain/com/example/Product.groovy      - Domain mapped to both datasources
src/
  integration-test/groovy/com/example/
    DetachedCriteriaCloneConnectionSpec.groovy - Integration tests proving the bug
```
