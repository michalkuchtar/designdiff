package com.powerapps.designdiff

import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class VerticalGuidesViewHolder(parentView: ViewGroup) : GuidesViewHolder(parentView, Mode.VERTICAL) {
    override val firstGuide: View by lazy { parentView.findViewById<View>(R.id.verticalGuideFirst) }
    override val secondsGuide: View by lazy { parentView.findViewById<View>(R.id.verticalGuideSecond) }
    override val guidesTextInfo: TextView by lazy { parentView.findViewById<TextView>(R.id.verticalGuidesInfo) }
}