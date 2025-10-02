package app.vishwakarma.neuroXcamera.ui.fragment

import androidx.recyclerview.widget.RecyclerView
import app.vishwakarma.neuroXcamera.databinding.GallerySlideBinding

class GallerySlide(val binding: GallerySlideBinding) : RecyclerView.ViewHolder(binding.root) {
    @Volatile var currentPostion = 0
}
