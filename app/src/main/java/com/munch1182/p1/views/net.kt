package com.munch1182.p1.views

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.google.gson.annotations.SerializedName
import com.munch1182.lib.base.withIO
import com.munch1182.lib.net.convert
import com.munch1182.lib.net.get
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.Items
import com.munch1182.p1.ui.SpacerV
import com.munch1182.p1.ui.theme.PagePaddingModifier
import com.munch1182.p1.ui.weight.Loading

@Composable
fun NetView() {
    ClickButton("当前IP") { showIp() }
}

private fun showIp() {
    DialogHelper.newBottom {
        var ipInfo by remember { mutableStateOf<IpInfo?>(null) }
        var weather by remember { mutableStateOf<Weather?>(null) }
        LaunchedEffect(null) {
            // https://api.techniknews.net
            withIO { ipInfo = get("https://api.techniknews.net/ipgeo").convert<IpInfo>() }
        }
        LaunchedEffect(ipInfo) {
            withIO {
                ipInfo?.let {
                    // https://open-meteo.com/
                    weather = get(
                        "https://api.open-meteo.com/v1/forecast?latitude=${it.lat}&longitude=${it.lon}" +
                                "&current=temperature_2m,wind_speed_10m,weather_code" +
                                "&daily=temperature_2m_max,temperature_2m_min,weather_code" +
                                "&timezone=${it.timezone}&forecast_days=1"
                    ).convert<Weather>()
                }
            }

        }
        Items(
            PagePaddingModifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 300.dp)
        ) {
            if (ipInfo == null) {
                Loading()
            } else {
                Text(ipInfo?.text ?: "")
                weather?.let {
                    SpacerV()
                    Text(it.text)
                }
            }
        }
    }.show()
}

private data class IpInfo(
    val continent: String,
    val country: String,
    val countryCode: String,
    val regionName: String,
    val city: String,
    val ip: String,
    val lat: Double,
    val lon: Double,
    val cached: Boolean,
    val isp: String,
    val org: String,
    val timezone: String
) {

    val text get() = "$ip\n$continent $country($countryCode) $regionName $city($lat, $lon) $timezone $isp $org "
}

private data class Weather(
    @SerializedName("current_units")
    val unit: CurrentUnits,
    val current: CurrentValue,
    @SerializedName("daily_units")
    val dayUnits: DayUnits,
    val daily: Daily
) {

    val text get() = text()

    private fun text(): String {
        val sb = StringBuilder()
        sb.appendLine("当前温度: ${current.temperature}${unit.temperature}")
            .appendLine("当前风速: ${current.windSpeed10m}${unit.windSpeed10m}")

        daily.time.forEachIndexed { index, time ->
            sb.appendLine(
                "$time: ${daily.temperature2mMin[index]}${dayUnits.temperatureMin} ~ ${daily.temperature2mMax[index]}${dayUnits.temperatureMax} " +
                        "${daily.weatherCode[index]}${dayUnits.weatherCode}"
            )
        }
        return sb.toString()
    }
}

private data class CurrentUnits(
    @SerializedName("temperature_2m")
    val temperature: String,
    @SerializedName("wind_speed_10m")
    val windSpeed10m: String,
)

private data class CurrentValue(
    @SerializedName("temperature_2m")
    val temperature: Float,
    @SerializedName("wind_speed_10m")
    val windSpeed10m: Float,
    @SerializedName("weather_code")
    val weatherCode: Int
)

private data class DayUnits(
    @SerializedName("temperature_2m_max")
    val temperatureMax: String,
    @SerializedName("temperature_2m_min")
    val temperatureMin: String,
    @SerializedName("weather_code")
    val weatherCode: String
)

private data class Daily(
    val time: List<String>,
    @SerializedName("temperature_2m_max")
    val temperature2mMax: List<Float>,
    @SerializedName("temperature_2m_min")
    val temperature2mMin: List<Float>,
    @SerializedName("weather_code")
    val weatherCode: List<Int>
)