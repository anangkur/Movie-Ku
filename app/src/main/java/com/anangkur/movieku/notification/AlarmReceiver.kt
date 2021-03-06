package com.anangkur.movieku.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.app.AlarmManager
import android.app.PendingIntent
import com.anangkur.movieku.BuildConfig
import com.anangkur.movieku.R
import com.anangkur.movieku.data.Injection
import com.anangkur.movieku.utils.Const
import com.anangkur.movieku.utils.Utils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.util.*

class AlarmReceiver : BroadcastReceiver(){

    override fun onReceive(p0: Context, p1: Intent) {
        val tempTitle = p1.getStringExtra(Const.EXTRA_ALARM_TITLE).orEmpty()
        val tempMessage = p1.getStringExtra(Const.EXTRA_ALARM_MESSAGE).orEmpty()
        val tempItemId = p1.getIntExtra(Const.EXTRA_NOTIF_ID, 0)
        if (tempItemId == Const.typeAlarmRelease){
            showNotifNewRelease(p0, tempTitle, tempMessage, tempItemId)
        }else{
            NotificationHelper(p0).createNoticication(tempTitle, tempMessage, tempItemId, null, null)
        }
    }

    private fun showNotifNewRelease(context: Context, title: String, message: String, itemId: Int){
        CoroutineScope(Dispatchers.IO).launch {
            val response = Injection.provideApiService(context).getTodayReleaseMovie(
                BuildConfig.apiKey,
                Utils.formatDateStandard(Utils.getTime()),
                Utils.formatDateStandard(Utils.getTime())
            )
            withContext(Dispatchers.Main){
                try {
                    if (response.results.isNotEmpty()){
                        val data = response.results[0]
                        val imageUrl = "${BuildConfig.baseImageUrl}${data.backdrop_path}"
                        val bitmap = Glide.with(context)
                            .asBitmap()
                            .load(imageUrl)
                            .apply(RequestOptions().centerCrop())
                            .apply(RequestOptions().transform(RoundedCorners(48)))
                            .submit()
                            .get()
                        NotificationHelper(context).createNoticication(title, data.original_title?:"", itemId, bitmap, data)
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    fun setupAlarm(context: Context, title: String, message: String, notifId: Int, time: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        intent.putExtra(Const.EXTRA_ALARM_TITLE, title)
        intent.putExtra(Const.EXTRA_ALARM_MESSAGE, message)
        intent.putExtra(Const.EXTRA_NOTIF_ID, notifId)

        val timeArray = time.split(":")

        val calendar = Calendar.getInstance()

        calendar.set(Calendar.HOUR_OF_DAY, timeArray[0].toInt())
        calendar.set(Calendar.MINUTE, timeArray[1].toInt())
        calendar.set(Calendar.SECOND, 0)

        val pendingIntent = PendingIntent.getBroadcast(context, notifId, intent, 0)

        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)
        Toast.makeText(context, context.resources.getString(R.string.message_alarm_activated), Toast.LENGTH_SHORT).show()
    }

    fun cancelAlarm(context: Context, type: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, type, intent, 0)
        pendingIntent.cancel()

        alarmManager.cancel(pendingIntent)

        Toast.makeText(context, context.resources.getString(R.string.message_alarm_deactivated), Toast.LENGTH_SHORT).show()
    }
}