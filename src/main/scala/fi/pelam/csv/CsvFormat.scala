package fi.pelam.csv

import java.nio.charset.Charset
import java.util.Locale

case class CsvFormat(charset: Charset, separator: Char, dataLocale: Locale, cellTypeLocale: Locale) {

}
