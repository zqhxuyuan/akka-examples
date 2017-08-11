package sample.events

import java.text.SimpleDateFormat
import java.util.Locale

object DateTimeUtils {

  val RAW_FORMAT = "dd/MMM/yyyy:HH:mm:ss Z"
  val TRANSFORMED_FORMAT = "yyyy-MM-dd HH:mm:ss"

  def format(dt: String): String = {
    val rawDateFormat = new SimpleDateFormat(RAW_FORMAT, Locale.US)
    val transformedDateFormat = new SimpleDateFormat(TRANSFORMED_FORMAT)
    transformedDateFormat.format(rawDateFormat.parse(dt))
  }
}