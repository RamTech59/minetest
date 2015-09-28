package net.minetest.minetest

public class MinetestAssetCopy : Activity()
{
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		
		setContentView(R.layout.assetcopy)
		
		m_ProgressBar = findViewById(R.id.progressBar1) as ProgressBar
		m_Filename = findViewById(R.id.textView1) as TextView
		
		val display = getWindowManager().getDefaultDisplay()
		m_ProgressBar.getLayoutParams().width = (display.getWidth() * 0.8).toInt()
		m_ProgressBar.invalidate()
		
		/* check if there's already a copy in progress and reuse in case it is*/
		val prevActivity = getLastNonConfigurationInstance() as MinetestAssetCopy?

		if (prevActivity != null)
		{
			m_AssetCopy = prevActivity.m_AssetCopy
		}
		else
		{
			m_AssetCopy = copyAssetTask()
			m_AssetCopy.execute()
		}
	}
	
	/* preserve asset copy background task to prevent restart of copying */
	/* this way of doing it is not recommended for latest android version */
	/* but the recommended way isn't available on android 2.x */
	override fun onRetainNonConfigurationInstance(): Any
	{
		return this
	}
	
	var m_ProgressBar: ProgressBar
	var m_Filename: TextView
	
	var m_AssetCopy: copyAssetTask
	
	private inner class copyAssetTask : AsyncTask<String, Integer, String>()
	{
		private fun getFullSize(filename: String): Long
		{
			var size: Long = 0
			try
			{
				val src = getAssets().open(filename)
				val buf = ByteArray(4096)
				
				var len = 0
				while ((len = src.read(buf)) > 0)
				{
					size += len.toLong()
				}
			} catch (e: IOException)
			{
				e.printStackTrace()
			}
			
			return size
		}
		

		override fun doInBackground(vararg files: String): String
		{
			m_foldernames = Vector<String>()
			m_filenames = Vector<String>()
			m_tocopy = Vector<String>()
			m_asset_size_unknown = Vector<String>()
			val baseDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/"
			
			
			// prepare temp folder
			val TempFolder = File(baseDir + "Minetest/tmp/")
			
			if (!TempFolder.exists())
			{
				TempFolder.mkdir()
			}
			else
			{
				val todel = TempFolder.listFiles()
				
				for (i in todel.indices)
				{
					Log.v("MinetestAssetCopy", "deleting: " + todel[i].getAbsolutePath())
					todel[i].delete()
				}
			}
			
			// add a .nomedia file
			try
			{
				val dst = FileOutputStream(baseDir + "Minetest/.nomedia")
				dst.close()
			} catch (e: IOException)
			{
				Log.e("MinetestAssetCopy", "Failed to create .nomedia file")
				e.printStackTrace()
			}
			
			
			// build lists from prepared data
			BuildFolderList()
			BuildFileList()
			
			// scan filelist
			ProcessFileList()
			
			// doing work
			m_copy_started = true
			m_ProgressBar.setMax(m_tocopy.size())
			
			for (i in m_tocopy.indices)
			{
				try
				{
					val filename = m_tocopy.get(i)
					publishProgress(i)
					
					var asset_size_unknown = false
					var filesize: Long = -1
					
					if (m_asset_size_unknown.contains(filename))
					{
						val testme = File(baseDir + "/" + filename)
						
						if (testme.exists())
						{
							filesize = testme.length()
						}

						asset_size_unknown = true
					}
					
					var src: InputStream
					try
					{
						src = getAssets().open(filename)
					} catch (e: IOException)
					{
						Log.e("MinetestAssetCopy", "Copying file: $filename FAILED (not in assets)")
						e.printStackTrace()
						continue
					}
					
					// Transfer bytes from in to out
					val buf = ByteArray(1 * 1024)
					var len = src.read(buf, 0, 1024)
					
					/* following handling is crazy but we need to deal with    */
					/* compressed assets.Flash chips limited livetime due to   */
					/* write operations, we can't allow large files to destroy */
					/* users flash.                                            */
					if (asset_size_unknown)
					{
						if ((len > 0) && (len < buf.size()) && (len == filesize))
						{
							src.close()
							continue
						}
						
						if (len == buf.size())
						{
							src.close()
							val size = getFullSize(filename)
							if (size == filesize)
							{
								continue
							}
							src = getAssets().open(filename)
							len = src.read(buf, 0, 1024)
						}
					}
					if (len > 0)
					{
						var total_filesize = 0
						val dst: OutputStream
						try
						{
							dst = FileOutputStream(baseDir + "/" + filename)
						} catch (e: IOException)
						{
							Log.e("MinetestAssetCopy", "Copying file: $baseDir/$filename FAILED (couldn't open output file)")
							e.printStackTrace()
							src.close()
							continue
						}
						
						dst.write(buf, 0, len)
						total_filesize += len
						
						while ((len = src.read(buf)) > 0)
						{
							dst.write(buf, 0, len)
							total_filesize += len
						}
						
						dst.close()
						Log.v("MinetestAssetCopy", "Copied file: " + m_tocopy.get(i) + " (" + total_filesize + " bytes)")
					}
					else if (len < 0)
					{
						Log.e("MinetestAssetCopy", "Copying file: " + m_tocopy.get(i) + " failed, size < 0")
					}
					src.close()
				} catch (e: IOException)
				{
					Log.e("MinetestAssetCopy", "Copying file: " + m_tocopy.get(i) + " failed")
					e.printStackTrace()
				}
				
			}
			return ""
		}
		
		
		/**
		 * update progress bar
		 */
		override fun onProgressUpdate(vararg progress: Integer)
		{
			
			if (m_copy_started)
			{
				var shortened = false
				var todisplay: String = m_tocopy.get(progress[0].toInt())
				m_ProgressBar.setProgress(progress[0].toInt())
				
				// make sure our text doesn't exceed our layout width
				val bounds = Rect()
				val textPaint = m_Filename.getPaint()
				textPaint.getTextBounds(todisplay, 0, todisplay.length(), bounds)
				
				while (bounds.width() > getResources().getDisplayMetrics().widthPixels * 0.7)
				{
					if (todisplay.length() < 2)
					{
						break
					}

					todisplay = todisplay.substring(1)
					textPaint.getTextBounds(todisplay, 0, todisplay.length(), bounds)
					shortened = true
				}
				
				if (!shortened)
				{
					m_Filename.setText(todisplay)
				}
				else
				{
					m_Filename.setText(".." + todisplay)
				}
			}
			else
			{
				var shortened = false
				var todisplay = m_Foldername
				var full_text = "scanning $todisplay ..."
				// make sure our text doesn't exceed our layout width
				val bounds = Rect()
				val textPaint = m_Filename.getPaint()
				textPaint.getTextBounds(full_text, 0, full_text.length(), bounds)
				
				while (bounds.width() > getResources().getDisplayMetrics().widthPixels * 0.7)
				{
					if (todisplay.length() < 2)
					{
						break
					}
					todisplay = todisplay.substring(1)
					full_text = "scanning $todisplay ..."
					textPaint.getTextBounds(full_text, 0, full_text.length(), bounds)
					shortened = true
				}
				
				if (!shortened)
				{
					m_Filename.setText(full_text)
				}
				else
				{
					m_Filename.setText("scanning ..$todisplay ...")
				}
			}
		}
		
		/**
		 * check al files and folders in filelist
		 */
		protected fun ProcessFileList()
		{
			val FlashBaseDir = Environment.getExternalStorageDirectory().getAbsolutePath()
			
			val itr = m_filenames.iterator()
			
			while (itr.hasNext())
			{
				val FlashPath = FlashBaseDir + "/" + itr.next()
				
				if (isAssetFolder(itr.next()))
				{
					/* store information and update gui */
					m_Foldername = itr.next()
					publishProgress(0)
					
					/* open file in order to check if it's a folder */
					val current_folder = File(FlashPath)
					if (!current_folder.exists())
					{
						if (!current_folder.mkdirs())
						{
							Log.e("MinetestAssetCopy", "\t failed create folder: " + FlashPath)
						}
						else
						{
							Log.v("MinetestAssetCopy", "\t created folder: " + FlashPath)
						}
					}
					
					continue
				}
				
				/* if it's not a folder it's most likely a file */
				var refresh = true
				
				val testme = File(FlashPath)
				
				var asset_filesize: Long = -1
				var stored_filesize: Long = -1
				
				if (testme.exists())
				{
					try
					{
						val fd = getAssets().openFd(itr.next())
						asset_filesize = fd.getLength()
						fd.close()
					} catch (e: IOException)
					{
						refresh = true
						m_asset_size_unknown.add(itr.next())
						Log.e("MinetestAssetCopy", "Failed to open asset file \"$FlashPath\" for size check")
					}
					
					stored_filesize = testme.length()
					
					if (asset_filesize == stored_filesize)
					{
						refresh = false
					}
					
				}
				
				if (refresh)
				{
					m_tocopy.add(itr.next())
				}
			}
		}
		
		/**
		 * read list of folders prepared on package build
		 */
		protected fun BuildFolderList()
		{
			try
			{
				val `is` = getAssets().open("index.txt")
				val reader = BufferedReader(InputStreamReader(`is`))
				
				var line: String? = reader.readLine()
				while (line != null) {
					m_foldernames.add(line)
					line = reader.readLine()
				}
				`is`.close()
			} catch (e1: IOException)
			{
				Log.e("MinetestAssetCopy", "Error on processing index.txt")
				e1.printStackTrace()
			}
			
		}
		
		/**
		 * read list of asset files prepared on package build
		 */
		protected fun BuildFileList()
		{
			var entrycount: Long = 0
			try
			{
				val `is` = getAssets().open("filelist.txt")
				val reader = BufferedReader(InputStreamReader(`is`))
				
				var line: String? = reader.readLine()
				while (line != null)
				{
					m_filenames.add(line)
					line = reader.readLine()
					entrycount++
				}
				`is`.close()
			} catch (e1: IOException)
			{
				Log.e("MinetestAssetCopy", "Error on processing filelist.txt")
				e1.printStackTrace()
			}
			
		}
		
		override fun onPostExecute(result: String?)
		{
			finish()
		}
		
		protected fun isAssetFolder(path: String): Boolean {
			return m_foldernames.contains(path)
		}
		
		var m_copy_started = false
		var m_Foldername = "media"
		var m_foldernames: Vector<String>
		var m_filenames: Vector<String>
		var m_tocopy: Vector<String>
		var m_asset_size_unknown: Vector<String>
	}
}
