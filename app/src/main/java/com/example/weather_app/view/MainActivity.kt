package com.example.weather_app.view

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.weather_app.BuildConfig
import com.example.weather_app.R
import com.example.weather_app.databinding.ActivityMainBinding
import com.example.weather_app.model.current_weather.CurrentWeather
import com.example.weather_app.model.forecast_weather.ForecastWeather
import com.example.weather_app.model.ip_geolocation.ipGeolocation
import com.example.weather_app.presenter.Presenter
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceTypes
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), View.OnClickListener, UpdateView {

    private lateinit var binding: ActivityMainBinding
    private lateinit var presenter: Presenter
    private lateinit var currentWeather: CurrentWeather
    private lateinit var forecastWeather: ForecastWeather

    private var city : Place? = null
    private lateinit var date : Calendar

    private val dateFormat : SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        //window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.white)

        Places.initialize(applicationContext, BuildConfig.Places_API_KEY, Locale.ENGLISH)
        Places.createClient(this)

        presenter = Presenter(this)
        presenter.updateGeolocation()

        binding.edittextLocation.isFocusable = false
        binding.edittextLocation.setOnClickListener(this)

    }

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    private fun fillScrollView() {
        var layer : View
        val inflater : LayoutInflater = layoutInflater

        val ds : DisplayMetrics = resources.displayMetrics
        val layoutHeight = 100 * ds.density
        val layoutWidth = 64 * ds.density
        val defaultMargin = 72 * ds.density
        var layoutMargin = 18 * ds.density

        val tempCalendar = date

        var forecastIdElem = 0

        for(i in 0..3) {
            layer = inflater.inflate(R.layout.small_weather_card, binding.placeForFutureDays, false)

            val params : ConstraintLayout.LayoutParams = ConstraintLayout.LayoutParams(
                ceil(layoutWidth).toInt(),
                ceil(layoutHeight).toInt())

            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            params.marginStart = layoutMargin.toInt()
            layer.layoutParams = params

            tempCalendar.add(Calendar.DATE, 1)


            layer.findViewById<TextView>(R.id.card_date).text = dateFormat.format(date.time)

            val tempCurr = forecastWeather.list[forecastIdElem]
            val tempTemperature = ((tempCurr.main.temp - 273.15).roundToInt())
            if(tempTemperature > 0) {
                layer.findViewById<TextView>(R.id.card_temperature).text = "+$tempTemperature"
            } else if(tempTemperature < 0) {
                layer.findViewById<TextView>(R.id.card_temperature).text = tempTemperature.toString()
            } else {
                layer.findViewById<TextView>(R.id.card_temperature).text = "0"
            }

            layer.findViewById<View>(R.id.card_weather_icon).background =
                SMALLweatherStateImage[tempCurr.weather[0].main]?.let { getDrawable(it) }
            forecastIdElem += 8

            binding.placeForFutureDays.addView(layer)
            layoutMargin += defaultMargin
        }

        layer = inflater.inflate(R.layout.small_weather_card, binding.placeForFutureDays, false)

        val params : ConstraintLayout.LayoutParams = ConstraintLayout.LayoutParams(
            ceil(layoutWidth).toInt(),
            ceil(layoutHeight).toInt())

        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        params.marginStart = layoutMargin.toInt()
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        params.marginEnd = ceil(18 * ds.density).toInt()

        layer.layoutParams = params
        layer.findViewById<TextView>(R.id.card_date).text = dateFormat.format(date.time)

        val tempCurr = forecastWeather.list[forecastIdElem]
        layer.findViewById<TextView>(R.id.card_temperature).text =
            ((tempCurr.main.temp - 273.15).roundToInt()).toString()

        layer.findViewById<View>(R.id.card_weather_icon).background =
            SMALLweatherStateImage[tempCurr.weather[0].main]?.let { getDrawable(it) }

        binding.placeForFutureDays.addView(layer)

    }

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    private fun changeMainCard() {
        if(city != null) binding.city.text = city?.name

        val tempTemperature = ((currentWeather.main.temp - 273.15).roundToInt())
        if(tempTemperature > 0) {
            binding.currentTemperature.text = "+$tempTemperature"
        } else if(tempTemperature < 0) {
            binding.currentTemperature.text = tempTemperature.toString()
        } else {
            binding.currentTemperature.text = "0"
        }

        binding.currentDate.text = dateFormat.format(date.time)
        binding.BIGweatherIcon.background =
            BIGweatherStateImage[currentWeather.weather[0].main]?.let {
            getDrawable(
                it
            )
        }

    }

    override fun onClick(v: View?) {

        val fields = listOf(Place.Field.NAME, Place.Field.ADDRESS)

        val intent = application?.let {
            Autocomplete.IntentBuilder(
                AutocompleteActivityMode.FULLSCREEN, fields)
                .setTypesFilter(Arrays.asList(PlaceTypes.CITIES))
                .build(it)
        }
        startActivityForResult(intent, 300)
    }

    @Override
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 300) {
            if (resultCode == RESULT_OK) {
                val place = Autocomplete.getPlaceFromIntent(data!!)
                city = place
                place.name?.let { Log.i("Hmm", it) }
                changeCity()
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Log.i("Error", "Error")
            } else if (resultCode == AutocompleteActivity.RESULT_CANCELED) {
                Log.i("Error", "Error: Cancel")
            }
        }
    }

    private fun changeCity() {
        binding.edittextLocation.hint = city?.address

        city?.name?.let { presenter.updateWeather(it, BuildConfig.OpenWeatherMap_API_KEY) }
    }

    override fun UpdateCurrentWeather(NEWcurrentWeather: CurrentWeather) {
        currentWeather = NEWcurrentWeather
        date = Calendar.getInstance()

        changeMainCard()
    }

    override fun UpdateForecastWeather(NEWforecastWeather: ForecastWeather) {
        forecastWeather = NEWforecastWeather
        date = Calendar.getInstance()

        binding.placeForFutureDays.removeAllViews()
        fillScrollView()
    }

    override fun UpdateGeolocation(NEWgeolocation: ipGeolocation) {
        binding.city.text = NEWgeolocation.city
        binding.edittextLocation.hint = NEWgeolocation.city + ", " + NEWgeolocation.country

        presenter.updateWeather(NEWgeolocation.city, BuildConfig.OpenWeatherMap_API_KEY)
    }

    val BIGweatherStateImage = mapOf(
        Pair("Clear", R.drawable.ic_big_clear_day),
        Pair("Clouds", R.drawable.ic_big_cloudy),
        Pair("Mist", R.drawable.ic_big_cloudy),
        Pair("Smoke", R.drawable.ic_big_cloudy),
        Pair("Haze", R.drawable.ic_big_cloudy),
        Pair("Dust", R.drawable.ic_big_cloudy),
        Pair("Fog", R.drawable.ic_big_cloudy),
        Pair("Sand", R.drawable.ic_big_cloudy),
        Pair("Ash", R.drawable.ic_big_cloudy),
        Pair("Squall", R.drawable.ic_big_cloudy),
        Pair("Tornado", R.drawable.ic_big_cloudy),
        Pair("Snow", R.drawable.ic_big_snow),
        Pair("Rain", R.drawable.ic_big_rain),
        Pair("Drizzle", R.drawable.ic_big_rain),
        Pair("Thunderstorm", R.drawable.ic_big_thunderstorm)
    )

    val SMALLweatherStateImage = mapOf(
        Pair("Clear", R.drawable.ic_small_clear_day),
        Pair("Clouds", R.drawable.ic_small_cloudy),
        Pair("Mist", R.drawable.ic_small_cloudy),
        Pair("Smoke", R.drawable.ic_small_cloudy),
        Pair("Haze", R.drawable.ic_small_cloudy),
        Pair("Dust", R.drawable.ic_small_cloudy),
        Pair("Fog", R.drawable.ic_small_cloudy),
        Pair("Sand", R.drawable.ic_small_cloudy),
        Pair("Ash", R.drawable.ic_small_cloudy),
        Pair("Squall", R.drawable.ic_small_cloudy),
        Pair("Tornado", R.drawable.ic_small_cloudy),
        Pair("Snow", R.drawable.ic_small_snow),
        Pair("Rain", R.drawable.ic_small_rain),
        Pair("Drizzle", R.drawable.ic_small_rain),
        Pair("Thunderstorm", R.drawable.ic_small_thunderstorm)
    )

}
