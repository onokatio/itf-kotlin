/**
 * オンラインショップの商品情報を示す、[itemId] をキーとするモデル。
 *
 * 名前や商品説明の他、商品につけられたコメントの一覧を保持する。
 */
class ShopItemModel(
    val itemId: ItemId,
    val name: String,
    val explanationText: String,
    val pictureUri: Uri,
    val commentIds: List<CommentId>
) {
    companion object {
        val ERROR_ITEM_MODEL: ShopItemModel = ShopItemModel(...)
    }
}

/**
 * 商品につけられたコメントの 1 エントリを示す、[commentId] をキーとするモデル。
 */
class CommentModel(
    val commentId: CommentId,
    val authorName: String,
    val commentText: String,
    val postDate: Date
)

/**
 * データモデルをローカルストレージに保存、およびローカルストレージから取得するインターフェース。
 */
interface LocalStore<K> {
    fun query(key: K): Any?
}

/**
 * [ShopItemModel] を保存・取得する [LocalStore]。
 */
class ShopItemStore(val commentStore: LocalStore<CommentId>) : LocalStore<ItemId> {
    override fun query(key: ItemId): ShopItemModel? {
        ...
    }
}

/**
 * [CommentModel] を保存・取得する [LocalStore]。
 */
class CommentStore : LocalStore<CommentId> {
    override fun query(key: CommentId): CommentModel {
        ...
    }
}

/**
 * URI で指定された Bitmap 画像を取得するクラス。
 * 取得した画像は、`capacityInMib` のサイズまでキャッシュされる。
 */
class ImageLoader(capacityInMib: Int) {
    private val cache: LruCache<Uri, ImageBitmap> = LruCache(capacityInMib)

    fun getImage(pictureUri: Uri, httpClientProvider: () -> HttpClient): ImageBitmap? {
        val cachedImage = cache[pictureUri]
        if (cachedImage != null) {
           return cachedImage
        }

        return getImageByHttp(httpClientProvider())
    }

    private fun getImageByHttp(httpClient: HttpClient): ImageBitmap? {
        ...
    }
}

val IMAGE_LOADER: ImageLoader = ImageLoader(500)

/**
 * 商品の詳細情報を [layout] に表示するクラス。
 *
 * [showItem] に `ItemId` を与えることで商品詳細を表示できる。
 */
class ShopItemPresenter(
    private val layout: ShopItemLayout,
    private val shopItemStore: ShopItemStore
) {
    private var currentShownItem: ShopItemModel? = null

    init {
        layout.purchaseButton.setButtonCallback { purchaseRequester.purchase(currentShownItem) }
    }

    fun CreateCommentElementById(commentId: CommentId): UiModel  {
        val comment = shopItemStore.commentStore.query(
            commentId
        ) as CommentModel
        return createCommentUiElement(comment)
    }
    fun showItem(itemId: ItemId) {
        val itemModel = shopItemStore.query(itemId)
        if (itemModel == null) {
            showErrorLayout()
            return
        }
        val commentElements = itemModel.commentIds.map { commentId -> CreateCommentElementById(commentId)}

        layout.itemNameUiElement.text = itemModel.name
        layout.itemDescriptionUiElement.text = itemModel.explanationText
        layout.itemPictureView.picture = IMAGE_LOADER.getImage(itemModel.pictureUri) { HTTP_CLIENT }
        layout.commentUiElementContainer.removeAll()
        layout.commentUiElementContainer.addUiElements(commentElements)
        currentShownItem = itemModel
    }

    fun showErrorLayout() {
        layout.itemNameUiElement.text = ShopItemModel.ERROR_ITEM_MODEL.name
        layout.itemDescriptionUiElement.text = ShopItemModel.ERROR_ITEM_MODEL.explanationText
        layout.itemPictureView.picture =
            IMAGE_LOADER.getImage(ShopItemModel.ERROR_ITEM_MODEL.pictureUri) { HTTP_CLIENT }
        layout.commentUiElementContainer.removeAll()

        showErrorDialog()
    }

    private fun createCommentUiElement(commentModel: CommentModel): UiElement { ... }

    private fun showErrorDialog() { ... }

    companion object {
        val HTTP_CLIENT: HttpClient = HttpClient()
    }
}

/**
 * 「商品の購入」リクエストを送出するクラス。
 *
 * 購入する商品を [setCurrentItemProvider] によって指定し、[purchase] でその商品購入のリクエストを送る。
 */
class PurchaseRequester {
    fun purchase(targetItem: ShopItemModel) {
        val currentItem = targetItem
        ...
    }
}
