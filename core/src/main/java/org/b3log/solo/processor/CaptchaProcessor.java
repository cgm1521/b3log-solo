/*
 * Copyright (c) 2009, 2010, 2011, 2012, B3log Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.b3log.solo.processor;

import java.io.BufferedInputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.b3log.latke.image.Image;
import org.b3log.latke.image.ImageService;
import org.b3log.latke.image.ImageServiceFactory;
import org.b3log.latke.servlet.HTTPRequestContext;
import org.b3log.latke.servlet.HTTPRequestMethod;
import org.b3log.latke.servlet.annotation.RequestProcessing;
import org.b3log.latke.servlet.annotation.RequestProcessor;
import org.b3log.latke.servlet.renderer.PNGRenderer;
import org.b3log.solo.SoloServletListener;

/**
 * Captcha processor.
 * 
 * <p>
 *    Checkout <a href="http://toy-code.googlecode.com/svn/trunk/CaptchaGenerator">
 *    the sample captcha generator</a> for more details.
 * </p>
 *
 * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
 * @version 1.1.0.2, Sep 18, 2012
 * @since 0.3.1
 */
@RequestProcessor
public final class CaptchaProcessor {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(CaptchaProcessor.class.getName());
    /**
     * Images service.
     */
    private static final ImageService IMAGE_SERVICE = ImageServiceFactory.getImageService();
    /**
     * Key of captcha.
     */
    public static final String CAPTCHA = "captcha";
    /**
     * Captchas.
     */
    private Image[] captchas;
    /**
     * Count of static captchas.
     */
    private static final int CAPTCHA_COUNT = 100;

    /**
     * Gets captcha.
     * 
     * @param context the specified context
     */
    @RequestProcessing(value = "/captcha.do", method = HTTPRequestMethod.GET)
    public void get(final HTTPRequestContext context) {
        final PNGRenderer renderer = new PNGRenderer();
        context.setRenderer(renderer);

        if (null == captchas) {
            loadCaptchas();
        }

        try {
            final HttpServletRequest request = context.getRequest();
            final HttpServletResponse response = context.getResponse();

            final Random random = new Random();
            final int index = random.nextInt(CAPTCHA_COUNT);
            final Image captchaImg = captchas[index];
            final String captcha = captchaImg.getName();

            final HttpSession httpSession = request.getSession(false);
            if (null != httpSession) {
                LOGGER.log(Level.FINER, "Captcha[{0}] for session[id={1}]", new Object[]{captcha, httpSession.getId()});
                httpSession.setAttribute(CAPTCHA, captcha);
            }

            response.setHeader("Pragma", "no-cache");
            response.setHeader("Cache-Control", "no-cache");
            response.setDateHeader("Expires", 0);

            renderer.setImage(captchaImg);
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Loads captcha.
     */
    private synchronized void loadCaptchas() {
        LOGGER.info("Loading captchas....");

        try {
            captchas = new Image[CAPTCHA_COUNT];

            final URL captchaURL = SoloServletListener.class.getClassLoader().getResource("captcha_static.zip");
            final ZipFile zipFile = new ZipFile(captchaURL.getFile());

            final Enumeration<? extends ZipEntry> entries = zipFile.entries();

            int i = 0;
            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();

                final BufferedInputStream bufferedInputStream = new BufferedInputStream(zipFile.getInputStream(entry));
                final byte[] captchaCharData = new byte[bufferedInputStream.available()];
                bufferedInputStream.read(captchaCharData);
                bufferedInputStream.close();

                final Image image = IMAGE_SERVICE.makeImage(captchaCharData);
                image.setName(entry.getName().substring(0, entry.getName().lastIndexOf('.')));

                captchas[i] = image;

                i++;
            }

            zipFile.close();
        } catch (final Exception e) {
            LOGGER.severe("Can not load captchs!");

            throw new IllegalStateException(e);
        }

        LOGGER.info("Loaded captch images");
    }
}
