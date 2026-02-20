package com.example

import grails.gorm.DetachedCriteria
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

@Integration
class DetachedCriteriaCloneConnectionSpec extends Specification {

    void "withConnection setting is lost after where() because clone() does not copy connectionName"() {
        given: "a product saved to secondary datasource"
        Product.secondary.withTransaction {
            new Product(name: 'secondary-product', price: 50.0).save(flush: true, failOnError: true)
        }

        and: "a different product saved to default datasource"
        Product.withTransaction {
            new Product(name: 'default-product', price: 200.0).save(flush: true, failOnError: true)
        }

        when: "creating a DetachedCriteria with withConnection('secondary')"
        DetachedCriteria<Product> baseCriteria = new DetachedCriteria<>(Product)
            .withConnection('secondary')

        then: "withConnection sets connectionName on the returned criteria"
        baseCriteria.@connectionName == 'secondary'

        when: "chaining where() on the criteria"
        DetachedCriteria<Product> criteria = baseCriteria.where { price > 100.0 }

        and: "executing the query"
        List<Product> results = Product.withTransaction {
            criteria.list()
        }

        then: """
            withConnection('secondary') should route to secondary datasource.
            Secondary only has price=50 (below 100), so results should be empty.

            BUG: clone() inside where() does not copy connectionName, so
            the query reverts to the default datasource (which has price=200),
            returning a wrong result.
        """
        results.size() == 1
        results[0].name == 'default-product'
    }

    void "withConnection setting is lost after max() because clone() does not copy connectionName"() {
        given: "products in secondary"
        Product.secondary.withTransaction {
            (1..5).each { i ->
                new Product(name: "secondary-${i}", price: i * 10.0).save(flush: true, failOnError: true)
            }
        }

        when: "creating criteria with withConnection then chaining max()"
        DetachedCriteria<Product> criteria = new DetachedCriteria<>(Product)
            .withConnection('secondary')
            .max(3)

        then: """
            After max(), the connectionName should still be 'secondary'.

            BUG: clone() inside max() does not copy connectionName,
            so it reverts to ConnectionSource.DEFAULT.
        """
        criteria.@connectionName == 'DEFAULT'

    }

    void "withConnection setting is lost after offset() because clone() does not copy connectionName"() {
        when: "creating criteria with withConnection then chaining offset()"
        DetachedCriteria<Product> criteria = new DetachedCriteria<>(Product)
            .withConnection('secondary')
            .offset(1)

        then: """
            After offset(), the connectionName should still be 'secondary'.

            BUG: clone() inside offset() does not copy connectionName,
            so it reverts to ConnectionSource.DEFAULT.
        """
        criteria.@connectionName == 'DEFAULT'

    }
}
