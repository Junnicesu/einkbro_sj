package info.plateaukao.einkbro.browser

import android.webkit.JavascriptInterface
import android.webkit.WebView

/**
 * JavaScript interface to detect HTML5 media (video/audio) playback state.
 * Injects listeners for play/pause/ended events on all media elements.
 */
class MediaPlaybackDetector(
    private val onMediaStateChanged: (isPlaying: Boolean) -> Unit
) {
    private var isMediaPlaying = false

    @JavascriptInterface
    fun onMediaPlay() {
        if (!isMediaPlaying) {
            isMediaPlaying = true
            onMediaStateChanged(true)
        }
    }

    @JavascriptInterface
    fun onMediaPause() {
        if (isMediaPlaying) {
            isMediaPlaying = false
            onMediaStateChanged(false)
        }
    }

    @JavascriptInterface
    fun onMediaEnded() {
        if (isMediaPlaying) {
            isMediaPlaying = false
            onMediaStateChanged(false)
        }
    }

    companion object {
        const val INTERFACE_NAME = "MediaPlaybackDetector"

        /**
         * JavaScript code to inject into webpages to monitor media playback.
         * This adds event listeners to all video and audio elements.
         */
        const val INJECTION_SCRIPT = """
            (function() {
                if (window.mediaPlaybackMonitorInstalled) return;
                window.mediaPlaybackMonitorInstalled = true;
                
                function attachMediaListeners(element) {
                    element.addEventListener('play', function() {
                        if (typeof MediaPlaybackDetector !== 'undefined') {
                            MediaPlaybackDetector.onMediaPlay();
                        }
                    });
                    
                    element.addEventListener('pause', function() {
                        if (typeof MediaPlaybackDetector !== 'undefined') {
                            MediaPlaybackDetector.onMediaPause();
                        }
                    });
                    
                    element.addEventListener('ended', function() {
                        if (typeof MediaPlaybackDetector !== 'undefined') {
                            MediaPlaybackDetector.onMediaEnded();
                        }
                    });
                }
                
                // Attach to existing video and audio elements
                document.querySelectorAll('video, audio').forEach(attachMediaListeners);
                
                // Monitor for dynamically added media elements
                const observer = new MutationObserver(function(mutations) {
                    mutations.forEach(function(mutation) {
                        mutation.addedNodes.forEach(function(node) {
                            if (node.nodeType === 1) {
                                if (node.tagName === 'VIDEO' || node.tagName === 'AUDIO') {
                                    attachMediaListeners(node);
                                }
                                // Check child elements
                                node.querySelectorAll && node.querySelectorAll('video, audio').forEach(attachMediaListeners);
                            }
                        });
                    });
                });
                
                observer.observe(document.body, {
                    childList: true,
                    subtree: true
                });
            })();
        """

        /**
         * Inject the media playback detector into a WebView
         */
        fun inject(webView: WebView, detector: MediaPlaybackDetector) {
            webView.addJavascriptInterface(detector, INTERFACE_NAME)
            webView.evaluateJavascript(INJECTION_SCRIPT, null)
        }
    }
}
