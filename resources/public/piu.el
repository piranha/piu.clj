;;; piu.el --- interface to paste.in.ua
;;; -*- mode: emacs-lisp -*-

;;; Copyright (c) 2010, 2015 Alexander Solovyov under new BSD License

;;; Author: Alexander Solovyov
;;; Version: 1.1
;;; URL: http://paste.in.ua/piu.el

;;; Commentary:
;;;
;;; Add autoload to your configuration file:
;;;
;;;    (autoload 'piu "piu" "paste buffer or region" t)
;;;
;;; And then use it like "M-x piu" or add a shortcut:
;;;
;;;    (global-set-key (kbd "C-x p") 'piu)
;;;
;;; Executing this will result in pasted region if there was any (depends on
;;; transient-mark-mode) or a whole buffer.
;;;
;;; In either case url of pasted text is left on the kill ring, the paste buffer
;;; and (probably) copied to system buffer.
;;;
;;; Code:


(defvar piu-url "https://paste.in.ua/"
  "Url to paste.in.ua or compatible service.")


(defvar piu-lexers
  '((nxml-mode . "xml")
    (html-mode . "xml")
    (emacs-lisp-mode . "lisp")
    (conf-windows-mode . "ini")
    (conf-unix-mode . "ini")
    (cs-mode . "csharp")
    (js2-mode . "js")))


(defun lexer-name ()
  "Return lexer name based on current major mode and 'piu-lexers'."
  (or (assoc-default major-mode piu-lexers)
      (replace-regexp-in-string
       "-" "" (substring (symbol-name major-mode) 0 -5))))


(defun piu-post (text)
  "Post TEXT to paste.in.ua."
  (let ((url-request-method "POST")
        (url-request-extra-headers
         '(("Content-Type" . "application/x-www-form-urlencoded")))
        (url-request-data
         (format "lexer=%s&data=%s"
                 (url-hexify-string (lexer-name))
                 (url-hexify-string text))))
    (url-retrieve piu-url 'piu-post-callback)))


(defun piu-post-callback (status)
  "Process POST results"
  (cond
   ((equal :error (car status))
    (message "request failure! %s" (cdr status)))

   ((equal :redirect (car status))
    (let ((paste-url (cadr status)))
      (kill-new paste-url)
      (message "%s, copied to clipboard" paste-url)))))


;;;###autoload
(defun piu (start end)
  "Paste the region (or whole buffer) to paste.in.ua.

URL returned is saved to 'kill-ring' (and, hopefully, to system buffer)."
  (interactive
   (if mark-active
       (list (region-beginning) (region-end))
     (list (point-min) (point-max))))
  (let ((selection (buffer-substring-no-properties start end)))
    (message "posting...")
    (piu-post selection)))


(provide 'piu)
;;; piu.el ends here
