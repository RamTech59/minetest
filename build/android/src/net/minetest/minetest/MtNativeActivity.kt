package net.minetest.minetest

public class MtNativeActivity : NativeActivity() {
	Override
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		dialogState = -1
		m_MessageReturnValue = ""
		
	}
	
	Override
	override fun onDestroy() {
		super.onDestroy()
	}
	
	
	public fun copyAssets() {
		val intent = Intent(this, MinetestAssetCopy::class.java)
		startActivity(intent)
	}
	
	public fun showDialog(acceptButton: String, hint: String, current: String,
	                      editType: Int) {
		
		val intent = Intent(this, MinetestTextEntry::class.java)
		val params = Bundle()
		params.putString("acceptButton", acceptButton)
		params.putString("hint", hint)
		params.putString("current", current)
		params.putInt("editType", editType)
		intent.putExtras(params)
		startActivityForResult(intent, 101)
		m_MessageReturnValue = ""
		dialogState = -1
	}
	
	public fun getDialogValue(): String {
		dialogState = -1
		return m_MessageReturnValue
	}
	
	public fun getDensity(): Float {
		return getResources().getDisplayMetrics().density
	}
	
	public fun getDisplayWidth(): Int {
		return getResources().getDisplayMetrics().widthPixels
	}
	
	public fun getDisplayHeight(): Int {
		return getResources().getDisplayMetrics().heightPixels
	}
	
	Override
	override fun onActivityResult(requestCode: Int, resultCode: Int,
	                              data: Intent?) {
		if (requestCode == 101) {
			if (resultCode == Activity.RESULT_OK) {
				val text = data!!.getStringExtra("text")
				dialogState = 0
				m_MessageReturnValue = text
			} else {
				dialogState = 1
			}
		}
	}
	
	public var dialogState: Int = 0
		private set
	private var m_MessageReturnValue: String? = null
	
	companion object {
		
		public fun putMessageBoxResult(text: String)
		
		init {
			System.loadLibrary("openal")
			System.loadLibrary("ogg")
			System.loadLibrary("vorbis")
			System.loadLibrary("ssl")
			System.loadLibrary("crypto")
			System.loadLibrary("gmp")
			
			// We don't have to load libminetest.so ourselves,
			// but if we do, we get nicer logcat errors when
			// loading fails.
			System.loadLibrary("minetest")
		}
	}
}/* ugly code to workaround putMessageBoxResult not beeing found */
