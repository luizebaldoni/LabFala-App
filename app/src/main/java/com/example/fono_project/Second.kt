package com.example.fono_project

// Bibliotecas necessárias para acesso a permissões, manipulação de mídia, interface gráfica e CameraX
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.bumptech.glide.Glide
import com.example.fono_project.databinding.ActivitySecondBinding
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Alias para o listener de luminosidade.
 * @param luma Valor médio da luminosidade da imagem.
 */

typealias LumaListener = (luma: Double) -> Unit

// Activity responsável pela captura de vídeo, manipulação de imagens GIF e análise de luminosidade.
class Second : AppCompatActivity() {
    // Vinculação com o layout via View Binding
    private lateinit var viewBinding: ActivitySecondBinding

    /**
     * Callback para o resultado da solicitação de permissões.
     * Verifica se todas as permissões foram concedidas e inicia a câmera.
     * Caso contrário, exibe uma mensagem e finaliza a Activity.
     */

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
    // Declaração de views e variáveis para manipulação de GIFs
    private lateinit var image_gifs: ImageView
    private var currentGifIndex = 0
    private val gifs = listOf(
        R.drawable.blusa, R.drawable.bola, R.drawable.bolacha, R.drawable.bruxa, R.drawable.carro,
        R.drawable.casa, R.drawable.cavalo, R.drawable.chave, R.drawable.chinelo, R.drawable.chupeta,
        R.drawable.chuva, R.drawable.dedo, R.drawable.dirigir, R.drawable.faca, R.drawable.feliz,
        R.drawable.galinha, R.drawable.gato, R.drawable.janela, R.drawable.lapis, R.drawable.leite,
        R.drawable.livro, R.drawable.macaco, R.drawable.mesa, R.drawable.nariz, R.drawable.olho,
        R.drawable.pato, R.drawable.porco, R.drawable.porta, R.drawable.prato, R.drawable.radio,
        R.drawable.rato, R.drawable.sapato, R.drawable.sapo, R.drawable.tenis, R.drawable.vaca, // fim da lista basica
        R.drawable.achou, R.drawable.balao, R.drawable.banana, R.drawable.barriga, R.drawable.boca,
        R.drawable.boneca, R.drawable.cabelo, R.drawable.cama, R.drawable.caminhao, R.drawable.chapeu,
        R.drawable.colher, R.drawable.copo, R.drawable.dente, R.drawable.elefante, R.drawable.lingua,
        R.drawable.leao, R.drawable.lua, R.drawable.mamadeira, R.drawable.meia, R.drawable.pe,
        R.drawable.pente, R.drawable.perna, R.drawable.peixe, R.drawable.sabonete, R.drawable.sol,
        R.drawable.suco, // fim da lista expandida
        )

    // Botões para navegação entre GIFs e variáveis para captura de imagem e vídeo
    private lateinit var next: ImageView
    private lateinit var back: ImageView
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService

    /**
     * Método chamado na criação da Activity.
     * Inicializa o layout, solicita permissões necessárias, configura a câmera,
     * inicializa botões para captura de vídeo e navegação entre GIFs.
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivitySecondBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Verifica se as permissões necessárias já foram concedidas, caso contrário, solicita
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
        // Define ação para o botão de captura de vídeo
        viewBinding.videoCaptureButton.setOnClickListener { captureVideo() }
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Inicializa views para exibição de GIFs e botões de navegação
        image_gifs = this.findViewById(R.id.image_gifs)
        next = this.findViewById(R.id.next)
        back = this.findViewById(R.id.back)

        // Carrega o primeiro GIF utilizando Glide
        Glide.with(this).load(gifs[currentGifIndex]).into(image_gifs)

        // Função interna para avançar para o próximo GIF
        fun nextGif() {
            currentGifIndex = (currentGifIndex + 1) % gifs.size
            Glide.with(this).load(gifs[currentGifIndex]).into(image_gifs)
        }
        // Define ação para o botão "next"
        next.setOnClickListener {
            nextGif()
        }
        // Função interna para voltar ao GIF anterior
        fun previousGif() {
            currentGifIndex = (currentGifIndex - 1 + gifs.size) % gifs.size
            Glide.with(this).load(gifs[currentGifIndex]).into(image_gifs)
        }
        // Define ação para o botão "back"
        back.setOnClickListener {
            previousGif()
        }
    }

    /**
     * Captura ou finaliza a gravação de vídeo.
     * Se já estiver gravando, para a gravação. Caso contrário, inicia uma nova gravação,
     * configurando o MediaStore para salvar o arquivo de vídeo.
     */

    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return
        viewBinding.videoCaptureButton.isEnabled = false
        val curRecording = recording
        if (curRecording != null) {
            // Para de capturar vídeo
            curRecording.stop()
            recording = null
            return
        }
        // Cria um nome baseado na data e hora atual
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        // Configura os valores para o arquivo de vídeo
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }
        // Configura as opções de saída para o MediaStore
        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        // Inicia a gravação de vídeo com as configurações definidas
        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .apply {
                // Habilita a captura de áudio se a permissão estiver concedida
                if (PermissionChecker.checkSelfPermission(
                        this@Second,
                        Manifest.permission.RECORD_AUDIO
                    ) ==
                    PermissionChecker.PERMISSION_GRANTED // Verifica a permissão
                ) {
                    withAudioEnabled() // Habilita áudio
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        // Atualiza o botão para indicar que a gravação está em andamento
                        viewBinding.videoCaptureButton.apply {
                            text = getString(R.string.stop_capture)
                            isEnabled = true
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        // Finaliza a gravação e verifica se houve erro
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
                                .show()
                            Log.d(TAG, msg)
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(
                                TAG, "Video capture ends with error: " +
                                        "${recordEvent.error}"
                            )
                        }
                        // Restaura o botão para o estado inicial
                        viewBinding.videoCaptureButton.apply {
                            text = getString(R.string.start_capture)
                            isEnabled = true
                        }
                    }
                }
            }
    }

    /**
     * Inicializa e configura a câmera utilizando a biblioteca CameraX.
     * Configura o preview, captura de imagem, análise de luminosidade e gravação de vídeo.
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            // Obtém o provider para vincular os casos de uso ao ciclo de vida
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Inicializa a captura de imagem
            imageCapture = ImageCapture.Builder().build()

            // Configura a análise de imagem para cálculo de luminosidade
            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                        Log.d(TAG, "Average luminosity: $luma")
                    })
                }
            // Configura o preview da câmera
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            // Configura o gravador para captura de vídeo com a melhor qualidade disponível
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            // Seleciona a câmera frontal como padrão
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            try {

                // Desvincula quaisquer casos de uso previamente ligados à câmera
                cameraProvider.unbindAll()

                // Vincula os casos de uso (preview, vídeo e análise) ao ciclo de vida
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Verifica se todas as permissões necessárias foram concedidas.
     * @return true se todas as permissões estiverem concedidas, false caso contrário.
     */

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Libera recursos ao destruir a Activity.
     * Encerra o executor da câmera.
     */

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    /**
     * Objeto companion contendo constantes utilizadas na Activity.
     */

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    /**
     * Classe interna responsável por analisar a luminosidade de uma imagem.
     * Implementa o analisador para uso com o ImageAnalysis da CameraX.
     * @param listener Callback que recebe o valor médio da luminosidade.
     **/
    private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {

        /**
         * Converte um ByteBuffer em um Array de Bytes.
         * @return Array de bytes representando os dados do buffer.
         */
        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Reinicia a posição do buffer para zero
            val data = ByteArray(remaining())
            get(data)   // Copia o conteúdo do buffer para o array
            return data // Retorna o array de bytes
        }

        /**
         * Analisa a imagem para calcular a luminosidade média.
         * Converte o buffer da primeira camada da imagem em um array de bytes,
         * calcula a média dos valores dos pixels e passa o resultado para o listener.
         * @param image Imagem capturada pela câmera.
         */
        override fun analyze(image: ImageProxy) {
            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map { it.toInt() and 0xFF }
            val luma = pixels.average()

            listener(luma)

            image.close()
        }
    }
}
