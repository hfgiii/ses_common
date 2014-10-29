package org.hfgiii.ses.common

import org.hfgiii.ses.common.admin.EsAdmin
import org.hfgiii.ses.common.dsl.response.readers.ResponseReaderDsl
import org.hfgiii.ses.common.load.EsIndexLoad


object SesCommon
       extends EsAdmin
       with    EsIndexLoad
       with    ResponseReaderDsl