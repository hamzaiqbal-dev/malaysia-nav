import OSLog

final class AboutController: MWMViewController {
  fileprivate struct AboutInfoTableViewCellModel {
    let title: String
    let image: UIImage?
    let didTapHandler: (() -> Void)?
  }

  private enum Constants {
    static let infoTableViewCellHeight: CGFloat = 40
  }

  private let scrollView = UIScrollView()
  private let stackView = UIStackView()
  private let logoImageView = UIImageView()
  private let headerTitleLabel = UILabel()
  private let additionalInfoStackView = UIStackView()
  private let osmView = OSMView()
  private let infoTableView = UITableView(frame: .zero, style: .plain)
  private let termsOfUseAndPrivacyPolicyView = ButtonsStackView()
  private var infoTableViewData = [AboutInfoTableViewCellModel]()
  private var onDidAppearCompletionHandler: (() -> Void)?

  init(onDidAppearCompletionHandler: (() -> Void)? = nil) {
    self.onDidAppearCompletionHandler = onDidAppearCompletionHandler
    super.init(nibName: nil, bundle: nil)
  }

  @available(*, unavailable)
  required init?(coder _: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  // MARK: - Lifecycle

  override func viewDidLoad() {
    super.viewDidLoad()
    setupViews()
    arrangeViews()
    layoutViews()
  }

  override func viewDidAppear(_ animated: Bool) {
    super.viewDidAppear(animated)
    if let completionHandler = onDidAppearCompletionHandler {
      completionHandler()
      onDidAppearCompletionHandler = nil
    }
  }
}

// MARK: - Private

private extension AboutController {
  func setupViews() {
    func setupTitle() {
      let titleView = UILabel()
      titleView.text = Self.formattedAppVersion()
      titleView.setFontStyle(.semibold17, color: .whitePrimary)
      titleView.isUserInteractionEnabled = true
      titleView.configureSingleLineAutoScaling()
      let titleDidTapGestureRecognizer = UITapGestureRecognizer(target: self, action: #selector(appVersionButtonTapped))
      titleView.addGestureRecognizer(titleDidTapGestureRecognizer)
      navigationItem.titleView = titleView
    }

    func setupScrollAndStack() {
      scrollView.delaysContentTouches = false
      scrollView.contentInset = UIEdgeInsets(top: 20, left: 0, bottom: 20, right: 0)

      stackView.axis = .vertical
      stackView.distribution = .fill
      stackView.alignment = .center
      stackView.spacing = 15
    }

    func setupLogo() {
      logoImageView.contentMode = .scaleAspectFit
      logoImageView.image = UIImage(named: "logo")
    }

    func setupHeaderTitle() {
      headerTitleLabel.setFontStyle(.semibold18, color: .blackPrimary)
      headerTitleLabel.text = L("about_headline")
      headerTitleLabel.textAlignment = .center
      headerTitleLabel.configureSingleLineAutoScaling()
    }

    func setupAdditionalInfo() {
      additionalInfoStackView.axis = .vertical
      additionalInfoStackView.spacing = 15

      [AboutInfo.noTracking, .noWifi, .community].forEach { additionalInfoStackView.addArrangedSubview(InfoView(image: nil, title: $0.title)) }
    }

    func setupOSM() {
      osmView.setMapDate(Self.formattedMapsDataVersion())
      osmView.didTapHandler = { [weak self] in
        self?.openUrl(AppConstants.openStreetMapURL)
      }
    }

    func setupInfoTable() {
      infoTableView.setStyle(.clearBackground)
      infoTableView.delegate = self
      infoTableView.dataSource = self
      infoTableView.separatorStyle = .none
      infoTableView.isScrollEnabled = false
      infoTableView.showsVerticalScrollIndicator = false
      infoTableView.contentInset = .zero
      infoTableView.register(cell: InfoTableViewCell.self)
    }

    func setupTermsAndPrivacy() {
      termsOfUseAndPrivacyPolicyView.addButton(title: L("privacy_policy"), didTapHandler: { [weak self] in
        self?.showBundledPage("privacy", title: L("privacy_policy"))
      })
      termsOfUseAndPrivacyPolicyView.addButton(title: L("terms_of_use"), didTapHandler: { [weak self] in
        self?.showBundledPage("terms", title: L("terms_of_use"))
      })
      termsOfUseAndPrivacyPolicyView.addButton(title: L("copyright"), didTapHandler: { [weak self] in
        self?.showCopyright()
      })
    }

    view.setStyle(.pressBackground)

    setupTitle()
    setupScrollAndStack()
    setupLogo()
    setupHeaderTitle()
    setupAdditionalInfo()
    setupOSM()
    setupInfoTable()
    setupTermsAndPrivacy()

    infoTableViewData = buildInfoTableViewData()
  }

  func arrangeViews() {
    view.addSubview(scrollView)
    scrollView.addSubview(stackView)
    stackView.addArrangedSubview(logoImageView)
    stackView.addArrangedSubview(headerTitleLabel)
    stackView.addArrangedSubviewWithSeparator(additionalInfoStackView)
    stackView.addArrangedSubviewWithSeparator(osmView)
    stackView.addArrangedSubviewWithSeparator(infoTableView)
    stackView.addArrangedSubviewWithSeparator(termsOfUseAndPrivacyPolicyView)
  }

  func layoutViews() {
    scrollView.translatesAutoresizingMaskIntoConstraints = false
    stackView.translatesAutoresizingMaskIntoConstraints = false
    logoImageView.translatesAutoresizingMaskIntoConstraints = false
    additionalInfoStackView.translatesAutoresizingMaskIntoConstraints = false
    infoTableView.translatesAutoresizingMaskIntoConstraints = false
    termsOfUseAndPrivacyPolicyView.translatesAutoresizingMaskIntoConstraints = false

    NSLayoutConstraint.activate([
      scrollView.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor),
      scrollView.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor),
      scrollView.topAnchor.constraint(equalTo: view.topAnchor),
      scrollView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
      scrollView.contentLayoutGuide.widthAnchor.constraint(equalTo: scrollView.frameLayoutGuide.widthAnchor),

      stackView.leadingAnchor.constraint(equalTo: scrollView.contentLayoutGuide.leadingAnchor, constant: 20),
      stackView.trailingAnchor.constraint(equalTo: scrollView.contentLayoutGuide.trailingAnchor, constant: -20),
      stackView.topAnchor.constraint(equalTo: scrollView.contentLayoutGuide.topAnchor),
      stackView.bottomAnchor.constraint(equalTo: scrollView.contentLayoutGuide.bottomAnchor),

      logoImageView.heightAnchor.constraint(equalToConstant: 64),
      logoImageView.widthAnchor.constraint(equalTo: logoImageView.heightAnchor),

      additionalInfoStackView.widthAnchor.constraint(equalTo: stackView.widthAnchor),

      osmView.widthAnchor.constraint(equalTo: stackView.widthAnchor),

      infoTableView.widthAnchor.constraint(equalTo: stackView.widthAnchor),
      infoTableView.heightAnchor.constraint(equalToConstant: Constants.infoTableViewCellHeight * CGFloat(infoTableViewData.count)),

      termsOfUseAndPrivacyPolicyView.widthAnchor.constraint(equalTo: stackView.widthAnchor),
    ])
  }

  func buildInfoTableViewData() -> [AboutInfoTableViewCellModel] {
    let infoContent: [AboutInfo] = [.faq, .reportMapDataProblem, .reportABug, .rateTheApp]
    return infoContent.map { [weak self] aboutInfo in
      AboutInfoTableViewCellModel(title: aboutInfo.title, image: aboutInfo.image, didTapHandler: {
        switch aboutInfo {
        case .faq:
          self?.navigationController?.pushViewController(FaqController(), animated: true)
        case .reportABug:
          MailComposer.sendBugReportWith(title: "Sampai Bug Report")
        case .reportMapDataProblem:
          self?.openUrl(aboutInfo.link)
        case .rateTheApp:
          RateUsManager.shared.showAppStoreReviewRequest()
        default:
          break
        }
      })
    }
  }

  /// Returns a human-readable maps data version.
  static func formattedMapsDataVersion() -> String {
    // First, convert version code like 220131 to a date.
    let df = DateFormatter()
    df.locale = Locale(identifier: "en_US_POSIX")
    df.dateFormat = "yyMMdd"
    let mapsVersionInt = FrameworkHelper.dataVersion()
    let mapsDate = df.date(from: String(mapsVersionInt))!
    // Second, print the date in the local user's format.
    df.locale = Locale.current
    df.dateStyle = .long
    df.timeStyle = .none
    return df.string(from: mapsDate)
  }

  static func formattedAppVersion() -> String {
    let appInfo = AppInfo.shared()
    // Use strong left-to-right unicode direction characters for the app version.
    return String(format: L("version"), "\u{2066}\(appInfo.bundleVersion)-\(appInfo.buildNumber)\u{2069}")
  }

  func showCopyright() {
    showBundledPage("copyright", title: L("copyright"))
  }

  /// Legal pages ship with the app, so they open offline and match the Android ones.
  func showBundledPage(_ name: String, title: String) {
    let path = Bundle.main.path(forResource: name, ofType: "html")!
    let html = try! String(contentsOfFile: path, encoding: String.Encoding.utf8)
    let webViewController = WebViewController(html: html, baseUrl: nil, title: title)!
    webViewController.openInSafari = true
    navigationController?.pushViewController(webViewController, animated: true)
  }

  func copyToClipboard(_ content: String) {
    UIPasteboard.general.string = content
    let message = String(format: L("copied_to_clipboard"), content)
    UIImpactFeedbackGenerator(style: .medium).impactOccurred()
    Toast.show(withText: message, alignment: .bottom, pinToSafeArea: false)
  }
}

// MARK: - Actions

private extension AboutController {
  @objc func appVersionButtonTapped() {
    copyToClipboard(Self.formattedAppVersion())
  }

  @objc func osmMapsDataButtonTapped() {
    copyToClipboard(Self.formattedMapsDataVersion())
  }
}

// MARK: - UITableViewDelegate

extension AboutController: UITableViewDelegate {
  func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
    tableView.deselectRow(at: indexPath, animated: true)
    infoTableViewData[indexPath.row].didTapHandler?()
  }

  func tableView(_: UITableView, heightForRowAt _: IndexPath) -> CGFloat {
    Constants.infoTableViewCellHeight
  }
}

// MARK: - UITableViewDataSource

extension AboutController: UITableViewDataSource {
  func tableView(_: UITableView, numberOfRowsInSection _: Int) -> Int {
    infoTableViewData.count
  }

  func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
    let cell = tableView.dequeueReusableCell(cell: InfoTableViewCell.self, indexPath: indexPath)
    let aboutInfo = infoTableViewData[indexPath.row]
    cell.set(image: aboutInfo.image, title: aboutInfo.title)
    return cell
  }
}

// MARK: - UIStackView + AddArrangedSubviewWithSeparator

private extension UIStackView {
  func addArrangedSubviewWithSeparator(_ view: UIView) {
    if !arrangedSubviews.isEmpty {
      let separator = UIView()
      separator.setStyleAndApply(.divider)
      separator.isUserInteractionEnabled = false
      separator.translatesAutoresizingMaskIntoConstraints = false
      addArrangedSubview(separator)
      NSLayoutConstraint.activate([
        separator.heightAnchor.constraint(equalToConstant: 1.0),
        separator.leadingAnchor.constraint(equalTo: leadingAnchor),
        separator.trailingAnchor.constraint(equalTo: trailingAnchor),
      ])
    }
    addArrangedSubview(view)
  }
}
