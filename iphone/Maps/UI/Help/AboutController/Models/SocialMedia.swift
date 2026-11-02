enum SocialMedia: CaseIterable {
  case email

  var link: String {
    switch self {
    case .email:
      return AppConstants.contactEmail
    }
  }

  var image: UIImage {
    switch self {
    case .email:
      return UIImage(named: "ic_social_media_mail")!
    }
  }
}
