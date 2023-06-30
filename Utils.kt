package com.imawo.veaguard.Helpers

import android.animation.*
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Build
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.GestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.gms.tasks.Task
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.messaging.FirebaseMessaging
import org.apache.commons.io.IOUtils
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.sql.CallableStatement
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Statement
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("StaticFieldLeak")
object Utils {
    var mError = ""
    var mCurrentAnimator: Animator? = null
    var mShortAnimationDuration = 200
    var scaleDown: ObjectAnimator? = null
    var scaleDownSecond: ObjectAnimator? = null
    var OutToRight: Animation? = null
    var OutToLeft: Animation? = null
    var InFromRight: Animation? = null
    var InFromLeft: Animation? = null

    fun initSwipeAnimation() {
        OutToRight = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, +1.0f,
            Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f
        )
        OutToRight?.setDuration(400)
        OutToRight?.setInterpolator(AccelerateDecelerateInterpolator())
        OutToLeft = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, -1.0f,
            Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f
        )
        OutToLeft?.setDuration(400)
        OutToLeft?.setInterpolator(AccelerateDecelerateInterpolator())
        InFromRight = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, +1.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
            Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f
        )
        InFromRight?.setDuration(400)
        InFromRight?.setInterpolator(AccelerateDecelerateInterpolator())
        InFromLeft = TranslateAnimation(
            Animation.RELATIVE_TO_PARENT, -1.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
            Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f
        )
        InFromLeft?.setDuration(400)
        InFromLeft?.setInterpolator(AccelerateDecelerateInterpolator())
    }

    fun animateTranslation(context: Context, mView: View, duration: Int, nCode: Int) {
        mView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        var translation = TranslateAnimation(0f, 0f, 0f, 0f)
        when (nCode) {
            0 -> translation =
                TranslateAnimation(0f, 0f, (getDisplayHeight(context) / 2).toFloat(), 0f)
            1 -> translation =
                TranslateAnimation(0f, 0f, (-1 * getDisplayHeight(context)).toFloat(), 0f)
            3 -> translation =
                TranslateAnimation(0f, 0f, 0f, (-1 * getDisplayHeight(context)).toFloat())
            2 -> translation = TranslateAnimation(
                0f, 0f, getDisplayHeight(context)
                    .toFloat(), 0f
            )
            4 -> translation = TranslateAnimation(
                0f, 0f, 0f, getDisplayHeight(context)
                    .toFloat()
            )
        }
        translation.startOffset = 0
        translation.duration = duration.toLong()
        translation.fillAfter = true
        translation.interpolator = DecelerateInterpolator()
        translation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                mView.setLayerType(View.LAYER_TYPE_NONE, null)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        mView.startAnimation(translation)
    }

    private fun getDisplayWidth(context: Context): Int {
        val metrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(metrics)
        return metrics.widthPixels
    }

    private fun getDisplayHeight(context: Context): Int {
        val metrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(metrics)
        return metrics.heightPixels
    }

    fun zooimageFromThumb(
        thumbView: View,
        expandedImageView: ImageView,
        container: RelativeLayout
    ) {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        if (mCurrentAnimator != null) {
            mCurrentAnimator!!.cancel()
        }

        // Load the high-resolution "zoomed-in" image.
        //expandedImageView.setImageResource(imageResId);

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        val startBounds = Rect()
        val finalBounds = Rect()
        val globalOffset = Point()

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        thumbView.getGlobalVisibleRect(startBounds)
        container.getGlobalVisibleRect(finalBounds, globalOffset)
        startBounds.offset(-globalOffset.x, -globalOffset.y)
        finalBounds.offset(-globalOffset.x, -globalOffset.y)

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        val startScale: Float
        if (finalBounds.width().toFloat() / finalBounds.height()
            > startBounds.width().toFloat() / startBounds.height()
        ) {
            // Extend start bounds horizontally
            startScale = startBounds.height().toFloat() / finalBounds.height()
            val startWidth = startScale * finalBounds.width()
            val deltaWidth = (startWidth - startBounds.width()) / 2
            startBounds.left -= deltaWidth.toInt()
            startBounds.right += deltaWidth.toInt()
        } else {
            // Extend start bounds vertically
            startScale = startBounds.width().toFloat() / finalBounds.width()
            val startHeight = startScale * finalBounds.height()
            val deltaHeight = (startHeight - startBounds.height()) / 2
            startBounds.top -= deltaHeight.toInt()
            startBounds.bottom += deltaHeight.toInt()
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        //thumbView.setAlpha(0f);
        thumbView.visibility = View.GONE
        expandedImageView.visibility = View.VISIBLE

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        expandedImageView.pivotX = 0f
        expandedImageView.pivotY = 0f

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        val set = AnimatorSet()
        set
            .play(
                ObjectAnimator.ofFloat(
                    expandedImageView, View.X,
                    startBounds.left.toFloat(), finalBounds.left.toFloat()
                )
            )
            .with(
                ObjectAnimator.ofFloat(
                    expandedImageView, View.Y,
                    startBounds.top.toFloat(), finalBounds.top.toFloat()
                )
            )
            .with(
                ObjectAnimator.ofFloat(
                    expandedImageView, View.SCALE_X,
                    startScale, 1f
                )
            )
            .with(
                ObjectAnimator.ofFloat(
                    expandedImageView,
                    View.SCALE_Y, startScale, 1f
                )
            )
        set.duration = mShortAnimationDuration.toLong()
        set.interpolator = DecelerateInterpolator()
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mCurrentAnimator = null
            }

            override fun onAnimationCancel(animation: Animator) {
                mCurrentAnimator = null
            }
        })
        set.start()
        mCurrentAnimator = set

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        /*final float startScaleFinal = startScale;
        expandedImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCurrentAnimator != null) {
                    mCurrentAnimator.cancel();
                }

                // Animate the four positioning/sizing properties in parallel,
                // back to their original values.
                AnimatorSet set = new AnimatorSet();
                set.play(ObjectAnimator
                        .ofFloat(expandedImageView, View.X, startBounds.left))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.Y,startBounds.top))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.SCALE_X, startScaleFinal))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.SCALE_Y, startScaleFinal));
                set.setDuration(mShortAnimationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        //thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        thumbView.setVisibility(View.VISIBLE);
                        mCurrentAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        //thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        thumbView.setVisibility(View.VISIBLE);
                        mCurrentAnimator = null;
                    }
                });
                set.start();
                mCurrentAnimator = set;
            }
        });*/
    }

    fun showMessage(view: View?, message: String?) {
        val snackbar = Snackbar
            .make(view!!, message!!, Snackbar.LENGTH_LONG)
        snackbar.show()
    }

    fun showDialog(context: Context?, title: String?, message: String?) {
        val builder = context?.let { MaterialAlertDialogBuilder(it) }
        builder?.setTitle(title)
        builder?.setMessage(message)
        builder?.setPositiveButton("OK") { dialog, which ->
            dialog.dismiss()
        }
        val alert = builder?.create()
        alert?.show()
        return
    }

    fun getDateFromDatePicker(datePicker: DatePicker): Date {
        val day = datePicker.dayOfMonth
        val month = datePicker.month
        val year = datePicker.year
        val calendar = Calendar.getInstance()
        calendar[year, month] = day
        return calendar.time
    }

    fun getCalendarFromDatePicker(datePicker: DatePicker): Calendar {
        val day = datePicker.dayOfMonth
        val month = datePicker.month
        val year = datePicker.year
        val calendar = Calendar.getInstance()
        calendar[year, month] = day
        return calendar
    }

    fun getCalendarFromCalendarView(calendarView: CalendarView): Calendar {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = calendarView.date
        return calendar
    }

    fun getDateFromText(textDate: String?, formatString: String?): Date? {
        val dateFormat = SimpleDateFormat(formatString)
        var date: Date? = Date()
        try {
            date = dateFormat.parse(textDate)
            return date
        } catch (e: ParseException) {
        }
        return null
    }

    fun getIndexArrayFromString(value: String?, arrayString: Array<Array<String>>): Int {
        //se cauta in arrayString index-ul matricii corespunzator Status
        var index = -1
        for (i in arrayString.indices) {
            if (arrayString[i][1].contains(value!!)) {
                index = i
            }
        }
        return index
    }

    fun getIndexArrayFromId(Id: Int, arrayString: Array<Array<String>>): Int {
        //se cauta in arrayString index-ul matricii corespunzator StatusId
        var index = -1
        for (i in arrayString.indices) {
            if (arrayString[i][0].contains(Id.toString())) {
                index = i
            }
        }
        return index
    }

    fun rotateView(view: View) {
        val rotate = RotateAnimation(
            0F,
            180F,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )
        rotate.duration = 5000
        rotate.interpolator = LinearInterpolator()
        rotate.repeatCount = ObjectAnimator.INFINITE
        view.startAnimation(rotate)
    }

    fun pulsateView(view: View?) {
        scaleDown = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat("scaleX", 0.9f),
            PropertyValuesHolder.ofFloat("scaleY", 0.9f)
        )
        scaleDown!!.duration = 310
        scaleDown!!.interpolator =
            FastOutSlowInInterpolator()
        scaleDown!!.repeatCount = ObjectAnimator.INFINITE
        scaleDown!!.repeatMode = ObjectAnimator.REVERSE
        scaleDown!!.start()
    }

    fun pulsateViewSecond(view: View?) {
        scaleDownSecond = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat("scaleX", 0.9f),
            PropertyValuesHolder.ofFloat("scaleY", 0.9f)
        )
        scaleDownSecond!!.duration = 310
        scaleDownSecond!!.interpolator =
            FastOutSlowInInterpolator()
        scaleDownSecond!!.repeatCount = ObjectAnimator.INFINITE
        scaleDownSecond!!.repeatMode = ObjectAnimator.REVERSE
        scaleDownSecond!!.start()
    }

    @JvmStatic
    fun pulsateClickView(view: View?) {
        scaleDown = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat("scaleX", 0.9f),
            PropertyValuesHolder.ofFloat("scaleY", 0.9f)
        )
        scaleDown!!.duration = 110
        scaleDown!!.interpolator =
            FastOutSlowInInterpolator()
        scaleDown!!.repeatCount = 1
        scaleDown!!.repeatMode = ObjectAnimator.REVERSE
        scaleDown!!.start()
    }

    fun isCurrentDay(compare: String): Boolean {
        val calendar = Calendar.getInstance()
        val day = calendar[Calendar.DAY_OF_MONTH]
        return String.format("%02d", day) == compare
    }

    fun isWeekEnd(date: Date?): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = date
        return calendar[Calendar.DAY_OF_WEEK] == Calendar.SATURDAY || calendar[Calendar.DAY_OF_WEEK] == Calendar.SUNDAY
    }

    fun loadUserImage(context: Context): Bitmap? {
        val sharedPref = context.getSharedPreferences(
            context.getString(R.string.preference_file_key), Context.MODE_PRIVATE
        )
        try {
            val base64String =
                sharedPref.getString(context.resources.getString(R.string.saved_image_file), "")
            val base64Image =
                base64String!!.split(",".toRegex()).toTypedArray()[1]
            val decodedString =
                Base64.decode(base64Image, Base64.DEFAULT)
            return ImageBase64.decodeBase64(base64Image, context)
        } catch (e: Exception) {
        }
        return null
    }

    fun setOnTouch(view: View, gesture: GestureDetector) {
        view.setOnTouchListener { v, event -> gesture.onTouchEvent(event) }
    }

    fun formatStringForWebView(
        mInputInternalNotes: TextInputLayout,
        mEditInternalNotes: EditText,
        content: String
    ): String {
        var content = content
        if (mInputInternalNotes.visibility == View.VISIBLE) {
            val multiLines = mEditInternalNotes.text.toString()
            val lines: Array<String>
            val delimiter = "\n"
            lines = multiLines.split(delimiter.toRegex()).toTypedArray()
            content = "<p>"
            for (i in lines.indices) {
                content += lines[i] + "<br>"
            }
            content += "</p>"
        } else {
            if (content.contains("<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\" />")) {
                content = content.substring(58)
            }
        }
        return content
    }

    fun replaceNull(input: String?): String {
        return input ?: ""
    }

    fun setImageViewCompressedJPEG(imageView: ImageView, drawable: Drawable) {
        val BYTE: ByteArray
        val bytearrayoutputstream = ByteArrayOutputStream()
        val bitmap1 = (drawable as BitmapDrawable).bitmap
        bitmap1.compress(Bitmap.CompressFormat.JPEG, 50, bytearrayoutputstream)
        BYTE = bytearrayoutputstream.toByteArray()
        val bitmap2 = BitmapFactory.decodeByteArray(BYTE, 0, BYTE.size)
        imageView.setImageBitmap(bitmap2)
    }

    @JvmStatic
    fun setViewCompressedJPEG(view: ViewGroup, drawable: Drawable) {
        val BYTE: ByteArray
        val bytearrayoutputstream = ByteArrayOutputStream()
        val bitmap1 = (drawable as BitmapDrawable).bitmap
        bitmap1.compress(Bitmap.CompressFormat.JPEG, 50, bytearrayoutputstream)
        BYTE = bytearrayoutputstream.toByteArray()
        val bitmap2 = BitmapFactory.decodeByteArray(BYTE, 0, BYTE.size)
        val drw: Drawable = BitmapDrawable(bitmap2)
        view.setBackgroundDrawable(drw)
    }

    fun isNumeric(strNumber: String?): Boolean {
        if (strNumber == null) {
            return false
        }
        try {
            val d = strNumber.toDouble()
        } catch (nfe: NumberFormatException) {
            return false
        }
        return true
    }

    fun round(value: Double, places: Int): Double {
        require(places >= 0)
        var bd = BigDecimal.valueOf(value)
        bd = bd.setScale(places, RoundingMode.HALF_UP)
        return bd.toDouble()
    }
}