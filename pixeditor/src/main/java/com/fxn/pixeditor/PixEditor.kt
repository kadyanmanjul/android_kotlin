package com.fxn.pixeditor

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.ViewPager
import com.fxn.pixeditor.imageeditengine.AnimationHelper
import com.fxn.pixeditor.imageeditengine.ImageEditActivity
import com.fxn.pixeditor.imageeditengine.ImageEditor
import com.fxn.pixeditor.imageeditengine.PhotoEditorFragment
import com.fxn.pixeditor.imageeditengine.adapters.FilterImageAdapter
import com.fxn.pixeditor.imageeditengine.adapters.PreviewImageAdapter
import com.fxn.pixeditor.imageeditengine.adapters.PreviewViewPagerAdapter
import com.fxn.pixeditor.imageeditengine.filter.ApplyFilterTask
import com.fxn.pixeditor.imageeditengine.filter.ProcessingImage
import com.fxn.pixeditor.imageeditengine.interfaces.OnSelectionStringListener
import com.fxn.pixeditor.imageeditengine.interfaces.WorkFinish
import com.fxn.pixeditor.imageeditengine.model.BitmapObject
import com.fxn.pixeditor.imageeditengine.model.ImageFilter
import com.fxn.pixeditor.imageeditengine.utils.*
import com.fxn.pixeditor.imageeditengine.views.PhotoEditorView
import com.fxn.pixeditor.imageeditengine.views.VerticalSlideColorPicker
import com.fxn.pixeditor.imageeditengine.views.ViewTouchListener
import com.fxn.pixeditor.imageeditengine.views.imagezoom.ImageViewTouch
import kotlinx.android.synthetic.main.activity_pix_editor.*
import java.io.Serializable


class PixEditor : AppCompatActivity(), View.OnClickListener, FilterImageAdapter.FilterImageAdapterListener,
    ViewTouchListener, Animation.AnimationListener, Serializable {


    private val requestCodePix: Int = 2201
    private lateinit var photoEditorView: PhotoEditorView
    private var currentMode: Int = 0
    private var filterLayoutHeight: Int = 0
    private lateinit var options: EditOptions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Utility.hideTopBar(this)
        setContentView(R.layout.activity_pix_editor)
        options = intent.getSerializableExtra(EDITOPTIONS) as EditOptions
        initialise()
    }

    val listBitmap = ArrayList<BitmapObject>()
    private lateinit var previewViewPagerAdapter: PreviewViewPagerAdapter
    private fun initialise() {
        if (options.selectedlist.size == 0) {
            Toast.makeText(this@PixEditor, "selectedlist", Toast.LENGTH_LONG).show()
        }
        previewViewPagerAdapter = PreviewViewPagerAdapter(this@PixEditor)
        listBitmap.clear()
        for (s in options.selectedlist) {
            listBitmap.add(BitmapObject(s))
        }
        resetupViewpagerAndBottomList()
        crop_btn.setOnClickListener(this)
        stickers_btn.setOnClickListener(this)
        add_text_btn.setOnClickListener(this)
        paint_btn.setOnClickListener(this)
        done_btn.setOnClickListener(this)
        back_iv.setOnClickListener(this)
        delete_image.setOnClickListener(this)
        add_more_btn.setOnClickListener(this)
        if (options.addMoreImagesListener == null) {
            add_more_btn.visibility = GONE
        }

        color_picker_view.setOnColorChangeListener(
            object : VerticalSlideColorPicker.OnColorChangeListener {
                override fun onColorChange(selectedColor: Int) {
                    if (currentMode == PhotoEditorFragment.MODE_PAINT) {
                        paint_btn.background = Utility.tintDrawable(this@PixEditor, R.drawable.circle2, selectedColor)
                        photoEditorView.color = (selectedColor)
                    } else if (currentMode == PhotoEditorFragment.MODE_ADD_TEXT) {
                        add_text_btn.background =
                            Utility.tintDrawable(this@PixEditor, R.drawable.circle2, selectedColor)
                        photoEditorView.setTextColor(selectedColor)
                    }
                }
            })

        val filterHelper = FilterHelper()
        filter_list_rv.layoutManager =
            LinearLayoutManager(this@PixEditor, LinearLayoutManager.HORIZONTAL, false)
        val filterImageAdapter = FilterImageAdapter(filterHelper.filters, this@PixEditor)
        filter_list_rv.adapter = filterImageAdapter


        filter_list_layout.post {
            filterLayoutHeight = filter_list_layout.height
            filter_list_layout.translationY = filterLayoutHeight.toFloat()
            mainViewPager.post {
                setupimageFilter(mainViewPager.currentItem)
            }
        }
        //  photoEditorView =   mainViewPager.getChildAt(0).findViewById(R.id.photo_editor_view) as PhotoEditorView

        mainViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                /*  if (scrollcheck){
                  var image_iv = mainViewPager.getChildAt(position).findViewById(R.id.image_iv) as ImageView
                  var photo_editor_view = mainViewPager.getChildAt(position).findViewById(R.id.photo_editor_view) as PhotoEditorView
                  filter_list_layout.animate().translationY(0f)
                  image_iv.animate().scaleY(0.7f)
                  photo_editor_view.animate().scaleY(0.7f)
                  filter_label.animate().alpha(0f)
                  done_btn.animate().alpha(0f)
                      scrollcheck = false
                  }*/
            }

            override fun onPageSelected(position: Int) {
                Log.e("position change", "-->>" + position)
                setupimageFilter(position)
            }
        })
    }

    fun setupimageFilter(position: Int) {
        filter_list_layout.post {
            filterLayoutHeight = filter_list_layout.height
            filter_list_layout.translationY = filterLayoutHeight.toFloat()
            photoEditorView =
                mainViewPager.getChildAt(position)
                    ?.findViewById(R.id.photo_editor_view) as PhotoEditorView
            val mainImageView = mainViewPager.getChildAt(position).findViewById(R.id.image_iv) as ImageViewTouch


            mainImageView.post {
                if (listBitmap[position].mainBitmap != null) {
                    val height = majorContainer!!.height
                    val layouparams = topPaddingView.layoutParams
                    layouparams.height = height / 2 - listBitmap[position].mainBitmap!!.height / 2
                    topPaddingView.layoutParams = layouparams
                    val layouparams1 = bottomPaddingView.layoutParams
                    layouparams1.height = height / 2 - listBitmap[position].mainBitmap!!.height / 2
                    bottomPaddingView.layoutParams = layouparams1
                    photoEditorView.setBounds(mainImageView.bitmapRect!!)
                    photoEditorView.setImageView(
                        bottomPaddingView, topPaddingView, mainImageView, delete_view,
                        this@PixEditor
                    )
                    photoEditorView.setOnTouchListener(
                        FilterTouchListener(
                            filter_list_layout,
                            filterLayoutHeight.toFloat(),
                            mainViewPager.getChildAt(position).findViewById(R.id.image_iv) as ImageView,
                            photoEditorView,
                            filter_label,
                            done_btn,
                            mainViewPager
                        )
                    )
                    (filter_list_rv.adapter as FilterImageAdapter).lastCheckedPostion =
                        listBitmap[position].filterSelection
                   /* GetFiltersTask(object : TaskCallback<ArrayList<ImageFilter>> {
                        override fun onTaskDone(data: ArrayList<ImageFilter>) {
                            val filterImageAdapter = filter_list_rv.adapter as FilterImageAdapter?
                            if (filterImageAdapter != null) {
                                filterImageAdapter.setData(data)
                                filterImageAdapter.notifyDataSetChanged()
                            }
                        }
                    }, listBitmap[position].mainBitmap!!).execute()*/
                }
            }

        }
    }

    companion object {
        @JvmField
        var IMAGE_RESULTS = "image_results"
        @JvmStatic
        val MODE_NONE = 0
        @JvmStatic
        val MODE_PAINT = 1
        @JvmStatic
        val MODE_ADD_TEXT = 2
        @JvmStatic
        val MODE_STICKER = 3
        @JvmStatic
        var EDITOPTIONS = "EDITOPTIONS"

        @JvmStatic
        fun start(context: Fragment, options: EditOptions) {
            PermUtil.checkForCamaraWritePermissions(context, object : WorkFinish {
                override fun onWorkFinish(check: Boolean?) {
                    val i = Intent(context.activity, PixEditor::class.java)
                    i.putExtra(EDITOPTIONS, options)
                    context.startActivityForResult(i, options.requestCode)
                }
            })
        }

        @JvmStatic
        fun start(context: FragmentActivity, options: EditOptions) {
            PermUtil.checkForCamaraWritePermissions(context, object : WorkFinish {
                override fun onWorkFinish(check: Boolean?) {
                    try {
                        val i = Intent(context, PixEditor::class.java)
                        i.putExtra(EDITOPTIONS, options)
                        context.startActivityForResult(
                            i,
                            options.requestCode
                        )
                    }catch (ex :Exception){
                        ex.printStackTrace()
                    }
                }
            })
        }
    }

    override fun onClick(v: View) {
        var num = 0
        when {
            v.id == R.id.crop_btn -> {
                setMode(MODE_NONE)
                /*  if (selectedFilter != null) {
                      val i = Intent(this, CropActivity::class.java)
                      i.putExtra(CropActivity.CROP_STRING, listBitmap[mainViewPager.currentItem].path)
                      startActivityForResult(i, CropActivity.CROP_NUM)
                  } else {
                      //  mListener!!.onCropClicked(getBitmapCache(listBitmap[mainViewPager.currentItem].orignalBitmap))
                      photoEditorView.hidePaintView()
                  }*/
                val i = Intent(this, CropActivity::class.java)
                i.putExtra(CropActivity.CROP_STRING, listBitmap[mainViewPager.currentItem].path)
                startActivityForResult(i, CropActivity.CROP_NUM)
            }
            v.id == R.id.stickers_btn -> setMode(MODE_STICKER)
            v.id == R.id.add_text_btn -> setMode(MODE_ADD_TEXT)
            v.id == R.id.paint_btn -> setMode(MODE_PAINT)
            v.id == R.id.back_iv -> onBackPressed()
            v.id == R.id.add_more_btn -> {
                setMode(MODE_NONE)
                var list = ArrayList<String>()
                for (s in listBitmap) {
                    list.add(s.path)
                }
                if (options.addMoreImagesListener != null) {
                   // options.addMoreImagesListener!!.addMore( list, requestCodePix)
                }
            }
            v.id == R.id.delete_image -> {
                num = mainViewPager.currentItem
                Log.e("check ", "num $num")
                val s = listBitmap.removeAt(num)
                resetupViewpagerAndBottomList()
                if (num == 0) {
                    num += 1
                } else {
                    num -= 1
                }
                mainViewPager.currentItem = num
            }
            v.id == R.id.done_btn -> {
                var resultList = ArrayList<String>()
                synchronized(this@PixEditor) {

                    for (count in 0 until listBitmap.size) {
                        var item = listBitmap[count]
                        synchronized(item) {

                            if (item.imageFilter != null) {
                                ApplyFilterTask(object : TaskCallback<Bitmap> {
                                    override fun onTaskDone(data: Bitmap) {
                                        if (data != null) {
                                            ProcessingImage(getBitmapCache(data, count),
                                                Utility.getCacheFilePath(this@PixEditor),
                                                object : TaskCallback<String> {
                                                    override fun onTaskDone(data: String) {
                                                        resultList.add(data)
                                                        Log.e("url data", "data" + data)
                                                        if (count == listBitmap.size - 1) {
                                                            val resultIntent = Intent()
                                                            resultIntent.putStringArrayListExtra(
                                                                IMAGE_RESULTS,
                                                                resultList
                                                            )
                                                            setResult(RESULT_OK, resultIntent)
                                                            finish()
                                                        }


                                                    }
                                                }).execute()
                                        }
                                    }
                                }, Bitmap.createBitmap(item.mainBitmap)).execute(item.imageFilter)
                            } else {
                                ProcessingImage(getBitmapCache(item.mainBitmap, count),
                                    Utility.getCacheFilePath(this@PixEditor),
                                    object : TaskCallback<String> {
                                        override fun onTaskDone(data: String) {
                                            resultList.add(data)
                                            Log.e("url data", "data" + data)
                                            if (count == listBitmap.size - 1) {
                                                val resultIntent = Intent()
                                                resultIntent.putStringArrayListExtra(
                                                    IMAGE_RESULTS,
                                                    resultList
                                                )
                                                setResult(RESULT_OK, resultIntent)
                                                finish()
                                            }
                                            Log.e("url data", "data" + data)
                                        }
                                    }).execute()
                            }
                            Log.e("url data", "seperator " + count)

                        }
                    }
                }
            }
        }
    }

    protected fun setMode(mode: Int) {
        var mode = mode
        if (currentMode != mode) {
            onModeChanged(mode)
        } else {
            mode = MODE_NONE
            onModeChanged(mode)
        }
        this.currentMode = mode
    }

    private fun onModeChanged(currentMode: Int) {
        Log.i(ImageEditActivity::class.java.simpleName, "CM: $currentMode")
        mainViewPager.scrollerEnabled = currentMode == 0
        onStickerMode(currentMode == PhotoEditorFragment.MODE_STICKER)
        onAddTextMode(currentMode == PhotoEditorFragment.MODE_ADD_TEXT)
        onPaintMode(currentMode == PhotoEditorFragment.MODE_PAINT)

        if (currentMode == PhotoEditorFragment.MODE_PAINT || currentMode == PhotoEditorFragment.MODE_ADD_TEXT) {
            AnimationHelper.animate(
                this@PixEditor, color_picker_view, R.anim.slide_in_right, View.VISIBLE,
                this@PixEditor
            )
        } else {
            AnimationHelper.animate(
                this@PixEditor, color_picker_view, R.anim.slide_out_right, View.INVISIBLE,
                this@PixEditor
            )
        }
    }

    override fun onFilterSelected(imageFilter: ImageFilter, pos: Int) {
        listBitmap[mainViewPager.currentItem].imageFilter = imageFilter
        var mainImageView =
            mainViewPager.getChildAt(mainViewPager.currentItem).findViewById(R.id.image_iv) as ImageViewTouch
        listBitmap[mainViewPager.currentItem].filterSelection = pos
        ApplyFilterTask(object : TaskCallback<Bitmap> {
            override fun onTaskDone(data: Bitmap) {
                if (data != null) {
                    mainImageView.setImageBitmap(data)
                }
            }
        }, Bitmap.createBitmap(listBitmap[mainViewPager.currentItem].mainBitmap)).execute(imageFilter)

    }

    private fun onAddTextMode(status: Boolean) {
        Log.e("----", "mainViewPager.scrollerEnabled " + mainViewPager.scrollerEnabled)

        if (status) {
            add_text_btn.background =
                Utility.tintDrawable(this@PixEditor, R.drawable.circle2, Color.parseColor("#03A9F4"))
            //photoEditorView.setTextColor(photoEditorView.getColor());
            photoEditorView.addText()

        } else {
            add_text_btn.background = null
            photoEditorView.hideTextMode()

        }
    }

    private fun onPaintMode(status: Boolean) {
        Log.e("----", "mainViewPager.scrollerEnabled " + mainViewPager.scrollerEnabled)

        if (status) {
            paint_btn.background = Utility.tintDrawable(this@PixEditor, R.drawable.circle2, Color.parseColor("#03A9F4"))
            photoEditorView.showPaintView()
            //paintEditView.setVisibility(View.VISIBLE);
        } else {
            paint_btn.background = null
            photoEditorView.hidePaintView()
            //photoEditorView.enableTouch(true);
            //paintEditView.setVisibility(View.GONE);
        }
    }

    private fun onStickerMode(status: Boolean) {

        Log.e("----", "mainViewPager.scrollerEnabled " + mainViewPager.scrollerEnabled)
        if (status) {
            stickers_btn.background =
                Utility.tintDrawable(this@PixEditor, R.drawable.circle2, Color.parseColor("#03A9F4"))
            if (this@PixEditor != null && this@PixEditor.intent != null) {
                var folderName = this@PixEditor.intent.getStringExtra(ImageEditor.EXTRA_STICKER_FOLDER_NAME)
                folderName = "stickers"
                photoEditorView.showStickers(folderName)
            }
        } else {
            stickers_btn.background = null
            photoEditorView.hideStickers()
        }
    }

    private fun getBitmapCache(bitmap: Bitmap?, pos: Int): Bitmap {
        Log.e("viewpager ", "child " + mainViewPager.childCount + "   current ->" + pos)
        var mainImageView = mainViewPager.getChildAt(pos).findViewById(R.id.image_iv) as ImageViewTouch
        var photo_editor_view = mainViewPager.getChildAt(pos).findViewById(R.id.photo_editor_view) as PhotoEditorView

        val touchMatrix = mainImageView.imageViewMatrix

        val resultBit = Bitmap.createBitmap(bitmap!!).copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBit)

        val data = FloatArray(9)
        touchMatrix.getValues(data)
        val cal = Matrix3(data)
        val inverseMatrix = cal.inverseMatrix()
        val m = Matrix()
        m.setValues(inverseMatrix.values)

        val f = FloatArray(9)
        m.getValues(f)
        val dx = f[Matrix.MTRANS_X].toInt()
        val dy = f[Matrix.MTRANS_Y].toInt()
        val scale_x = f[Matrix.MSCALE_X]
        val scale_y = f[Matrix.MSCALE_Y]
        canvas.save()
        canvas.translate(dx.toFloat(), dy.toFloat())
        canvas.scale(scale_x, scale_y)

        photo_editor_view.isDrawingCacheEnabled = true
        if (photo_editor_view.drawingCache != null) {
            canvas.drawBitmap(photo_editor_view.drawingCache, 0f, 0f, null)
        }

        if (photo_editor_view.paintBit != null) {
            canvas.drawBitmap(photo_editor_view.paintBit, 0f, 0f, null)
        }

        canvas.restore()
        return resultBit
    }

    override fun onStartViewChangeListener(view: View) {
        Log.i(ImageEditActivity::class.java.simpleName, "onStartViewChangeListener" + "" + view.id)
        toolbar_layout.visibility = View.GONE
        AnimationHelper.animate(this@PixEditor, delete_view, R.anim.fade_in_medium, View.VISIBLE, this@PixEditor)
    }

    override fun onStopViewChangeListener(view: View) {
        Log.i(ImageEditActivity::class.java.simpleName, "onStopViewChangeListener" + "" + view.id)
        delete_view.visibility = View.GONE
        AnimationHelper.animate(this@PixEditor, toolbar_layout, R.anim.fade_in_medium, View.VISIBLE, this@PixEditor)
    }

    override fun onStartViewFullChangeListener(view: View) {
        AnimationHelper.animate(this@PixEditor, toolbar_layout, R.anim.fadeout, View.GONE, this@PixEditor)
        AnimationHelper.animate(this@PixEditor, done_btn, R.anim.fadeout, View.GONE, this@PixEditor)

    }

    override fun onStopViewFullChangeListener(view: View) {
        AnimationHelper.animate(this@PixEditor, toolbar_layout, R.anim.fade_in_medium, View.VISIBLE, this@PixEditor)
        AnimationHelper.animate(this@PixEditor, done_btn, R.anim.fade_in_medium, View.VISIBLE, this@PixEditor)

    }

    override fun onAnimationRepeat(animation: Animation?) {
    }

    override fun onAnimationEnd(animation: Animation?) {
    }

    override fun onAnimationStart(animation: Animation?) {
    }

    public override fun onActivityResult(requestCode1: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode1, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode1 == CropActivity.CROP_NUM) {
            //  var b = data!!.getByteArrayExtra(("cropdata")
            val bytes = data!!.getByteArrayExtra("cropdata") as ByteArray
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            listBitmap[mainViewPager.currentItem].orignalBitmap = bmp
            listBitmap[mainViewPager.currentItem].mainBitmap = bmp
            setupimageFilter(mainViewPager.currentItem)
        }
        if (resultCode == Activity.RESULT_OK && requestCode1 == requestCodePix) {
            val list = data!!.getStringArrayListExtra("image_results")
            val Extraimages = ArrayList<BitmapObject>()
            for (l in list) {
                var check = true
                Log.e("new List", "-> " + l)
                for (s in listBitmap) {
                    if (l == s.path) {
                        Log.e("bitmap check", "-> " + s.path)
                        Extraimages.add(s)
                        check = false
                    }
                }
                if (check) {
                    Extraimages.add(BitmapObject(l))
                }
            }
            listBitmap.clear()
            listBitmap.addAll(Extraimages)
            resetupViewpagerAndBottomList()
        }
    }

    private fun resetupViewpagerAndBottomList() {
        previewViewPagerAdapter.list.clear()
        previewViewPagerAdapter.list.addAll(listBitmap)
        mainViewPager.adapter = previewViewPagerAdapter
        mainViewPager.offscreenPageLimit = options.selectedlist.size
        if (options.selectedlist.size > 1) {
            editRecyclerView.visibility = View.VISIBLE
            val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            var previewImageAdapter = PreviewImageAdapter(this)
            var list = ArrayList<String>()
            for (s in listBitmap) {
                list.add(s.path)
            }
            previewImageAdapter.addImage(list)
            previewImageAdapter.addOnSelectionListener(object : OnSelectionStringListener {
                override fun onClick(Img: String, view: View, position: Int) {
                    Log.e("position", "recycleview $position")

                    mainViewPager.currentItem = position
                }
            })

            editRecyclerView.apply {
                layoutManager = linearLayoutManager
                adapter = previewImageAdapter
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val myFabSrc = getDrawable(R.drawable.ic_done)
        val willBeWhite = myFabSrc?.constantState?.newDrawable()
        willBeWhite?.mutate()?.setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY)
        done_btn.setImageDrawable(willBeWhite)
        done_btn.setColorFilter(Color.WHITE)

    }
}
