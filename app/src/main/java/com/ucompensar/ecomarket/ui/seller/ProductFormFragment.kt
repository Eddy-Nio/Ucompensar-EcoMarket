package com.ucompensar.ecomarket.ui.seller

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ucompensar.ecomarket.R
import com.ucompensar.ecomarket.data.Session
import com.ucompensar.ecomarket.data.model.CATEGORIES
import com.ucompensar.ecomarket.data.model.Product
import com.ucompensar.ecomarket.data.repository.ProductRepository
import com.ucompensar.ecomarket.databinding.FragmentProductFormBinding
import com.ucompensar.ecomarket.util.launchSafely
import com.ucompensar.ecomarket.util.loadProductImage
import com.ucompensar.ecomarket.util.show
import com.ucompensar.ecomarket.util.toast
import java.io.File

/**
 * Formulario de creación y edición de productos. La foto se toma con la cámara o se elige de la
 * galería y se sube a Firebase Storage al guardar. Sin ID crea un producto; con ID lo edita.
 */
class ProductFormFragment : Fragment() {

    private var _binding: FragmentProductFormBinding? = null
    private val binding get() = _binding!!
    private var product: Product? = null
    private var newImage: Uri? = null
    private var pendingCameraUri: Uri? = null

    /** Selector de fotos del sistema (no requiere permiso de almacenamiento). */
    private val pickFromGallery = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(::setNewImage)
    }

    /** App de cámara del sistema; guarda la foto en un archivo temporal compartido por FileProvider. */
    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) pendingCameraUri?.let(::setNewImage)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentProductFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val productId = arguments?.getString(ARG_ID)
        activity?.title = getString(if (productId == null) R.string.seller_new_product else R.string.seller_edit_product)
        binding.acPCategory.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, CATEGORIES))
        binding.txtReviewNote.show(productId == null)
        binding.btnPDelete.show(productId != null)

        binding.btnGallery.setOnClickListener {
            pickFromGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.btnCamera.setOnClickListener { openCamera() }
        binding.btnPSave.setOnClickListener { save() }
        binding.btnPDelete.setOnClickListener { confirmDelete() }

        if (productId != null) {
            launchSafely(::setBusy) { ProductRepository.get(productId)?.let(::fill) }
        }
    }

    /** Rellena el formulario con el producto que se va a editar. */
    private fun fill(existing: Product) {
        product = existing
        binding.etPName.setText(existing.name)
        binding.etPDesc.setText(existing.description)
        binding.etPPrice.setText(existing.price.toString())
        binding.etPStock.setText(existing.stock.toString())
        binding.acPCategory.setText(existing.category, false)
        if (newImage == null) binding.imgPreview.loadProductImage(existing.imageUrl)
    }

    private fun openCamera() {
        val dir = File(requireContext().cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "product_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
        pendingCameraUri = uri
        takePhoto.launch(uri)
    }

    private fun setNewImage(uri: Uri) {
        newImage = uri
        binding.imgPreview.setImageURI(uri)
    }

    /** Valida los campos y guarda en Firestore (y la foto en Storage). */
    private fun save() {
        val name = binding.etPName.text.toString().trim()
        val price = binding.etPPrice.text.toString().toIntOrNull()
        val stock = binding.etPStock.text.toString().toIntOrNull()
        val category = binding.acPCategory.text.toString()
        val required = getString(R.string.error_required)

        binding.tilPName.error = if (name.isEmpty()) required else null
        binding.tilPPrice.error = if (price == null || price <= 0) required else null
        binding.tilPStock.error = if (stock == null || stock < 0) required else null
        binding.tilPCategory.error = if (category !in CATEGORIES) required else null
        if (listOf(binding.tilPName, binding.tilPPrice, binding.tilPStock, binding.tilPCategory).any { it.error != null }) return
        if (newImage == null && product?.imageUrl.isNullOrBlank()) {
            toast(getString(R.string.product_photo_required))
            return
        }

        val seller = Session.current
        val draft = (product ?: Product(sellerId = seller.uid, sellerName = seller.business.ifBlank { seller.name })).copy(
            name = name, description = binding.etPDesc.text.toString().trim(),
            price = price!!, stock = stock!!, category = category
        )
        launchSafely(::setBusy) {
            ProductRepository.save(requireContext(), draft, newImage)
            toast(getString(R.string.product_saved))
            parentFragmentManager.popBackStack()
        }
    }

    private fun confirmDelete() {
        val current = product ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete_title)
            .setMessage(getString(R.string.confirm_delete_product, current.name))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                launchSafely(::setBusy) {
                    ProductRepository.delete(current)
                    toast(getString(R.string.product_deleted))
                    parentFragmentManager.popBackStack()
                }
            }
            .show()
    }

    private fun setBusy(busy: Boolean) {
        _binding ?: return
        binding.progress.visibility = if (busy) View.VISIBLE else View.INVISIBLE
        binding.btnPSave.isEnabled = !busy
        binding.btnPDelete.isEnabled = !busy
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ID = "product_id"

        fun newInstance(id: String?) = ProductFormFragment().apply {
            arguments = Bundle().apply { putString(ARG_ID, id) }
        }
    }
}
