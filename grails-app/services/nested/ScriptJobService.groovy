package nested

import grails.gorm.transactions.Transactional


@Transactional
class ScriptJobService {

  def execute(boolean fail = false) {
    if (fail) {
      throw new RuntimeException('Eeek')
    }
  }

}
