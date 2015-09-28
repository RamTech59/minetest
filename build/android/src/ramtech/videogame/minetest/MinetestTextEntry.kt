package net.minetest.minetest

public class MinetestTextEntry : Activity() {
	public var mTextInputDialog: AlertDialog
	public var mTextInputWidget: EditText
	
	private val MultiLineTextInput = 1
	private val SingleLineTextInput = 2
	private val SingleLinePasswordInput = 3

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		val b = getIntent().getExtras()
		val acceptButton = b.getString("EnterButton")
		val hint = b.getString("hint")
		val current = b.getString("current")
		val editType = b.getInt("editType")
		
		val builder = AlertDialog.Builder(this)
		mTextInputWidget = EditText(this)
		mTextInputWidget.setHint(hint)
		mTextInputWidget.setText(current)
		mTextInputWidget.setMinWidth(300)
		if (editType == SingleLinePasswordInput) {
			mTextInputWidget.setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
		} else {
			mTextInputWidget.setInputType(InputType.TYPE_CLASS_TEXT)
		}
		
		
		builder.setView(mTextInputWidget)
		
		if (editType == MultiLineTextInput)
		{
			builder.setPositiveButton(acceptButton, object : DialogInterface.OnClickListener
			{
				override fun onClick(dialog: DialogInterface, whichButton: Int)
				{
					pushResult(mTextInputWidget.getText().toString())
				}
			})
		}
		
		builder.setOnCancelListener(object : DialogInterface.OnCancelListener {
			override fun onCancel(dialog: DialogInterface) {
				cancelDialog()
			}
		})
		
		mTextInputWidget.setOnKeyListener(object : OnKeyListener
		{
			override fun onKey(view: View, KeyCode: Int, event: KeyEvent): Boolean
			{
				if (KeyCode == KeyEvent.KEYCODE_ENTER)
				{
					
					pushResult(mTextInputWidget.getText().toString())
					return true
				}

				return false
			}
		})
		
		mTextInputDialog = builder.create()
		mTextInputDialog.show()
	}
	
	public fun pushResult(text: String)
	{
		val resultData = Intent()
		resultData.putExtra("text", text)
		setResult(Activity.RESULT_OK, resultData)
		mTextInputDialog.dismiss()
		finish()
	}
	
	public fun cancelDialog()
	{
		setResult(Activity.RESULT_CANCELED)
		mTextInputDialog.dismiss()
		finish()
	}
}
