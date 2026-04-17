package nested

import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class ScriptJobSpec extends Specification implements DomainUnitTest<ScriptJob> {

    def setup() {
    }

    def cleanup() {
    }

    void "test something"() {
        expect:"fix me"
            true == false
    }
}
