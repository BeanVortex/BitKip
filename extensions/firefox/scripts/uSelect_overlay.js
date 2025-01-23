/*
 * uSelect iDownload
 *
 * Copyright © 2011-2013 Alessandro Guido
 * Copyright © 2011-2013 Marco Palumbo
 *
 * This file is part of uSelect iDownload.
 *
 * uSelect iDownload is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * uSelect iDownload is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with uSelect iDownload.  If not, see <http://www.gnu.org/licenses/>.
 */

var uSelect_iDownload;

// Listen for messages from the content script
browser.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message.type === 'toggleOverlay')
        uSelect_iDownload.toggle();
});


(function () {

    CSS = {
        ids: {
            glass: 'ileabdhfjmgaognikmjgmhhkjffggejc-glass',
            help: 'ileabdhfjmgaognikmjgmhhkjffggejc-help',
            overlay: 'ileabdhfjmgaognikmjgmhhkjffggejc-overlay'
        },

        classes: {
            closing: 'ileabdhfjmgaognikmjgmhhkjffggejc-closing',
            exiting: 'ileabdhfjmgaognikmjgmhhkjffggejc-exiting',
            hidden: 'ileabdhfjmgaognikmjgmhhkjffggejc-hidden',
            inverted: 'ileabdhfjmgaognikmjgmhhkjffggejc-inverted',
            invisible: 'ileabdhfjmgaognikmjgmhhkjffggejc-invisible',
            loading: 'ileabdhfjmgaognikmjgmhhkjffggejc-loading',
            relative: 'ileabdhfjmgaognikmjgmhhkjffggejc-relative',
            selected: 'ileabdhfjmgaognikmjgmhhkjffggejc-selected',
            selectionRectangle: 'ileabdhfjmgaognikmjgmhhkjffggejc-selection-rectangle'
        }
    };

    function Overlay() {
        this._invertedSelection = false; // remove element from selection if true

        this._hideKeyCode = 72; // "H"

        /* current position of the mouse pointer */
        this._curpos = { x: 0, y: 0 };

        /* all elements of the document that can be opened or downloaded */
        this._selectableElements = [];

        /*
         * elements currently visibile in the browser window viewport.
         * Used to optimize drawSelection()
         */
        this._visibleElements = [];

        var that = this;

        /* contains all things in the extension and covers 100% of the page */
        var overlay = document.createElement('div');
        overlay.id = CSS.ids.overlay;

        /*
         * This overlay is above selected links and has the only purpose of
         * avoiding hover events
         */
        var glass = document.createElement('div');
        glass.id = CSS.ids.glass;

        /* div that shows usage instructions */
        var helpDiv = document.createElement('div');
        helpDiv.id = CSS.ids.help;
        helpDiv.innerHTML = browser.i18n.getMessage('usage');

        /* These shouldn't be needed */
        helpDiv.onmouseover = function () { helpDiv.classList.add(CSS.classes.invisible); }
        helpDiv.onmouseout = function () { helpDiv.classList.remove(CSS.classes.invisible); }

        var grabEvent = function (e) {
            e.preventDefault();
            e.stopPropagation();
        };

        var updateMousePosition = function (e) {
            /* coordinates from the top-left corner of the viewport */
            /* this function is very hot, keep as fast as possible */
            that._curpos.x = e.clientX;
            that._curpos.y = e.clientY;
        };

        /*
         * 0 - left click
         * 1 - ctrl + left click
         * 2 - right click
         */
        var pressedButton = null;

        /* handlers for page events */
        var handlers = {
            contextmenu: function (e) {
                grabEvent(e);
            },

            mousemove: function (e) {
                /* since this handlers get esecuted lots of times is attached only
                   when selecting or deselecting */
                updateMousePosition(e);
                that.sm.fireEvent('mousemove');
            },

            mousedown: function (e) {
                if (pressedButton !== null)
                    return;

                if (e.button != 0 && e.button != 2)
                    return;

                pressedButton = e.button == 0 ? (e.ctrlKey ? 1 : 0) : 2;
                switch (pressedButton) {
                    case 0: /* left button */
                        updateMousePosition(e);
                        that.sm.fireEvent('mousedown');
                        break;
                    case 1: /* ctrl + left click */
                    case 2: /* right button */
                        updateMousePosition(e);
                        that.sm.fireEvent('alt_mousedown');
                        break;
                }
                grabEvent(e);
            },

            mouseup: function (e) {
                if (pressedButton === null || (e.button != pressedButton && e.button != 0 && pressedButton != 1))
                    return;

                switch (e.button) {
                    case 0:
                        updateMousePosition(e);
                        that.sm.fireEvent(pressedButton == 1 ? 'alt_mouseup' : 'mouseup');
                        break;
                    case 2:
                        updateMousePosition(e);
                        that.sm.fireEvent('alt_mouseup');
                        break;
                }
                pressedButton = null;
                grabEvent(e);
            },

            keydown: function (e) {
                switch (e.keyCode) {
                    case 13: // Enter
                        that.sm.fireEvent('req_download');
                        break;
                    case that._hideKeyCode:
                        that.sm.fireEvent('hide_key_down');
                        break;
                    case 27: // Esc
                        that.sm.fireEvent('req_exit');
                        break;
                    default:
                        return;
                }

                grabEvent(e);
            },

            keyup: function (e) {
                switch (e.keyCode) {
                    case that._hideKeyCode:
                        that.sm.fireEvent('hide_key_up');
                        break;
                    default:
                        return;
                }

                grabEvent(e);
            },

            resize: (function () {
                var timeout = null;

                var fun = function () {
                    timeout = null;
                    that.updateVisibleElements();
                }

                return function (e) {
                    if (timeout !== null)
                        clearTimeout(timeout);
                    timeout = setTimeout(fun, 100);
                }
            })(),

            scroll: (function () {
                var timeout = null;

                var fun = function () {
                    timeout = null;
                    that.updateVisibleElements();
                }

                return function (e) {
                    if (timeout !== null)
                        clearTimeout(timeout);
                    timeout = setTimeout(fun, 100);
                }
            })()
        };

        document.addEventListener('contextmenu', handlers.contextmenu);
        document.addEventListener('scroll', handlers.scroll);
        window.addEventListener('resize', handlers.resize);

        /*
         * Keyup/down event handler need to be run in the capture
         * phase so that they can prevent the propagation
         */
        document.addEventListener('keydown', handlers.keydown, true);
        document.addEventListener('keyup', handlers.keyup, true);

        /*
         * mousedown/mouseup events must be grabbed to avoid the browser
         * starting to select something.
         *
         * we need to capture these events only on the body, otherwise
         * they would catch i.e. clicking the scrollbar
         */
        document.body.addEventListener('mousedown', handlers.mousedown, true);
        document.body.addEventListener('mouseup', handlers.mouseup, true);

        /*
         * How the overlay is structured
         *
         * The overlay works with a stack of layers
         * body
         *   - div#overlay
         *     - div#glass
         *     - div#help
         *     - div#selection-rectangle
         */
        overlay.appendChild(helpDiv);
        overlay.appendChild(glass);


        var statemachine = new StateMachine();
        /******************************************************************************/
        statemachine.states['load'] = {
            __enter__: function () {
                document.documentElement.classList.add(CSS.classes.loading);
                document.body.appendChild(overlay);

				overlay.addEventListener('transitionend', () => {
                    overlay.removeEventListener('transitionend', arguments.callee);
					that.sm.fireEvent('load_done');
                });

                /*
                 * We have to let the DOM to notice that the element has been added
                 * before adding the class, otherwise the transition does not kick
                 * in (the div is loaded already with opacity = 1)
                 */
				queueMicrotask(() => {
					     that.populate();
                    that.updateVisibleElements();
                    document.documentElement.classList.remove(CSS.classes.loading);
				});
            },

            load_done: 'idle',
        };
        /******************************************************************************/
        statemachine.states['exit'] = {
            __enter__: function () {
                document.removeEventListener('mousemove', handlers.mousemove);
                document.removeEventListener('scroll', handlers.scroll);
                window.removeEventListener('resize', handlers.resize);
                document.removeEventListener('contextmenu', handlers.contextmenu);

                document.removeEventListener('keydown', handlers.keydown, true);
                document.removeEventListener('keyup', handlers.keyup, true);

                document.body.removeEventListener('mousedown', handlers.mousedown, true);
                document.body.removeEventListener('mouseup', handlers.mouseup, true);

                that._selectableElements.forEach(function (el) {
                    el._private.delegate.classList.remove(CSS.classes.selected);
                    el._private.delegate.classList.remove(CSS.classes.relative);
                    delete el._private;
                });

                overlay.addEventListener('transitionend', function () {
                    overlay.removeEventListener('transitionend', arguments.callee);
                    document.body.removeChild(overlay);
                    document.documentElement.classList.remove(CSS.classes.exiting);
                    that.sm.fireEvent('exit_done');
                });
                document.documentElement.classList.add(CSS.classes.exiting);
            },

            __exit__: function () {
                uSelect_iDownload.instance = null;
            },

            exit_done: null,
        };
        /******************************************************************************/
        statemachine.states['idle'] = {
            mousedown: 'selection',
            alt_mousedown: 'deselection',
            req_exit: 'exit',
            req_download: 'action-download',
            hide_key_down: 'hidden',
        };
        /******************************************************************************/
        var redrawInterval = null;
        var recalcInterval = null;

        var selection_common_in = function () {
            var div = document.createElement('div');
            div.classList.add(CSS.classes.selectionRectangle);
            div.style.left = that._curpos.x + 'px';
            div.style.top = that._curpos.y + 'px';
            if (that._invertedSelection)
                div.classList.add(CSS.classes.inverted);
            that._cursel = document.body.appendChild(div);

            that._startpos = { x: that._curpos.x, y: that._curpos.y };
            that._selrect = { x: that._curpos.x, y: that._curpos.y, w: 0, h: 0 };

            /*
             * Avoid redrawing the same selection rectangle multiple times
             */
            that._lastDrawn = {};
            redrawInterval = setInterval(that.drawSelection.bind(that), 30);
            recalcInterval = setInterval(that.calcSelectedElements.bind(that), 30);
            document.addEventListener('mousemove', handlers.mousemove);
        };

        var selection_common_out = function () {
            document.removeEventListener('mousemove', handlers.mousemove);
            clearInterval(redrawInterval);
            clearInterval(recalcInterval);
            redrawInterval = null;
            recalcInterval = null;
            that.calcSelectedElements();

            for (var i in that._selectableElements) {
                var priv = that._selectableElements[i]._private;
                priv.selected = priv.selected2;
            }

            /*
             * There can be multiple selection rectangle if the user clicks while
             * one is fading out
             */
            that._cursel.addEventListener('transitionend', function () {
                /* the bind() is necessary since _cursel is nullified a few lines down */
                document.body.removeChild(this);
            }.bind(that._cursel));
            that._cursel.classList.add(CSS.classes.closing);
            delete that._cursel;
            delete that._startpos;
            delete that._selrect;
            delete that._lastDrawn;
            return 'idle';
        };

        var selection_common_mousemove = function () {
            /* update selection rectangle. This is used by:
             * - drawSelection
             * - calcSelectedElements
             * - highlightSelected
             */
            if (!that._startpos || !that._curpos) {
                return;
            }
            var p0 = that._startpos, p1 = that._curpos, r = that._selrect;
            r.x = Math.min(p0.x, p1.x);
            r.y = Math.min(p0.y, p1.y);
            r.w = Math.abs(p0.x - p1.x);
            r.h = Math.abs(p0.y - p1.y);
        };

        statemachine.states['selection'] = {
            __enter__: function () {
                that._invertedSelection = false;
                selection_common_in();
            },

            mousemove: selection_common_mousemove,
            mouseup: selection_common_out,
            req_exit: 'exit',
        };

        statemachine.states['deselection'] = {
            __enter__: function () {
                that._invertedSelection = true;
                selection_common_in();
            },

            mousemove: selection_common_mousemove,
            alt_mouseup: selection_common_out,
            req_exit: 'exit',
        };
        /******************************************************************************/
        statemachine.states['action-download'] = {
            __enter__: function () {
                var urls = selectedElementUrls(that._selectableElements);
                browser.runtime.sendMessage({
                    'type': 'downloadBatch',
                    'urls': urls
                });
                that.sm.fireEvent('done');
            },

            done: 'exit',
        };
        /******************************************************************************/
        statemachine.states['hidden'] = {
            __enter__: function () {
                document.documentElement.classList.add(CSS.classes.hidden);
            },

            __exit__: function () {
                document.documentElement.classList.remove(CSS.classes.hidden);
                that.updateVisibleElements();
            },

            hide_key_up: 'idle',
            req_exit: 'exit',
        };
        /******************************************************************************/
        this.sm = statemachine;
    }

    Overlay.prototype.populate = function () {
        var seq = document.links;
        var javascript_re = /^javascript:/;

        for (var i = 0; i < seq.length; i++) {
            var el = seq[i];

            if (el.href == undefined || javascript_re.test(el.href))
                continue;

            var delegate = el;
            var innerImgs = el.getElementsByTagName('img');
            if (innerImgs.length)
                delegate = innerImgs[0]; // FIXME make this more generic

            el._private = {
                selected: false,
                selected2: false,
                delegate: delegate,
                positionfix: false
            };
            this._selectableElements.push(el);
        }
    };

    Overlay.prototype.updateVisibleElements = function () {
        var viewport = {
            x: 0,
            y: 0,
            w: window.innerWidth,
            h: window.innerHeight
        };
        var visible = this._visibleElements;
        var selectable = this._selectableElements;

        visible.splice(0);

        for (var i in selectable) {
            var el = selectable[i], priv = el._private, del = priv.delegate;
            var boundingRect = del.getBoundingClientRect();

            if (intersects(boundingRect, viewport)) {
                visible.push(el);
                if (!priv.positionfix) {
                    priv.positionfix = true;
                    if (window.getComputedStyle(del).position == 'static')
                        del.classList.add(CSS.classes.relative);
                }
            }
        }
    };

    Overlay.prototype.drawSelection = function () {
        var rect = this._selrect, last = this._lastDrawn, style = this._cursel.style;

        if (rect.x == last.x
            && rect.y == last.y
            && rect.w == last.w
            && rect.h == last.h)
            return;

        style.left = (last.x = rect.x) + 'px';
        style.top = (last.y = rect.y) + 'px';
        style.width = (last.w = rect.w) + 'px';
        style.height = (last.h = rect.h) + 'px';
    };

    function batchUpdateClasses(add, remove) {
        for (var i in add)
            add[i].classList.add(CSS.classes.selected);
        for (var i in remove)
            remove[i].classList.remove(CSS.classes.selected);
    }

    Overlay.prototype.calcSelectedElements = function () {
        var seq = this._visibleElements, selRect = this._selrect;
        var toadd = [], toremove = [];

        for (var i in seq) {
            var priv = seq[i]._private, del = priv.delegate;

            var clientRects = priv.delegate.getClientRects();
            var elemIntersects = false;
            for (var j = 0; j < clientRects.length; j++) {
                var r = clientRects[j];

                if (intersects(r, selRect)) {
                    elemIntersects = true;
                    break;
                }
            }

            var new_status = elemIntersects ? !this._invertedSelection : priv.selected;
            if (new_status != priv.selected2) {
                (new_status ? toadd : toremove).push(del);
                priv.selected2 = new_status;
            }
        }
        if (toadd || toremove)
			queueMicrotask(() => batchUpdateClasses(toadd, toremove));
    };

    function intersects(clientRect, rect) {
        if (clientRect.bottom < rect.y
            || rect.y + rect.h < clientRect.top
            || clientRect.right < rect.x
            || rect.x + rect.w < clientRect.left)
            return false;
        return true;
    }

    function selectedElementUrls(elements) {
        var tmp = {};
        elements.forEach(function (el) {
            if (el._private.selected)
                tmp[el.href] = null;
        });
        return Object.keys(tmp);
    }

    uSelect_iDownload = {
        instance: null,
        toggle: function () {
            if (uSelect_iDownload.instance === null) {
                uSelect_iDownload.instance = new Overlay();
                uSelect_iDownload.instance.sm.start('load');
            } else {
                uSelect_iDownload.instance.sm.fireEvent('req_exit');
            }
        }
    }


    return { status: 'loaded' };
})();